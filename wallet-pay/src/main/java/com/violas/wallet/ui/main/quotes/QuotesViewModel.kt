package com.violas.wallet.ui.main.quotes

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.content.ContextProvider
import com.palliums.utils.coroutineExceptionHandler
import com.palliums.utils.toMutableMap
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ExchangeManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.WrongPasswordException
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.socket.ExchangeSocket
import com.violas.wallet.repository.socket.Subscriber
import com.violas.wallet.ui.main.quotes.bean.ExchangeToken
import com.violas.wallet.ui.main.quotes.bean.IOrder
import com.violas.wallet.ui.main.quotes.bean.IToken
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.palliums.violascore.wallet.Account
import org.palliums.violascore.wallet.KeyPair
import java.math.BigDecimal
import java.math.RoundingMode

class QuotesViewModel(application: Application) : AndroidViewModel(application), Subscriber {
    // 是否开启兑换功能
    val isEnable = MutableLiveData(false)
    // 当前选择的币种
    val currentFormCoinLiveData = MutableLiveData<IToken>()
    val currentToCoinLiveData = MutableLiveData<IToken>()
    // 当前兑换的币种
    val currentExchangeCoinLiveData = MediatorLiveData<IToken>()
    // 是否是正向兑换
    val isPositiveChangeLiveData = MutableLiveData(true)
    // 汇率
    val exchangeRateLiveData = MediatorLiveData<String>()
    val exchangeRateNumberLiveData = MediatorLiveData<BigDecimal>()
    private val exchangeRate = MutableLiveData("... = ...")
    // 当前委托
    val meOrdersLiveData = MutableLiveData<List<IOrder>>()
    // 他人委托
    val isShowMoreAllOrderLiveData = MutableLiveData(false)
    val allDisplayOrdersLiveData = MediatorLiveData<List<IOrder>>()
    private val allOrdersLiveData = MutableLiveData<List<IOrder>>()
    private val mShowMoreAllOrderMaxCount = 20
    private val mShowMoreAllOrderMinCount = 5
    // 兑换数量
    val mFromCoinAmountLiveData = MutableLiveData<String>()
    val mToCoinAmountLiveData = MutableLiveData<String>()
    // Token 列表
    private val mTokenList = ArrayList<IToken>()

    private val mTokenManager by lazy {
        TokenManager()
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    private val mExchangeManager by lazy {
        ExchangeManager()
    }

    private var mAccount: AccountDO? = null

    init {
        EventBus.getDefault().register(this)
        ExchangeSocket.addSubscriber(this)
        handleAccountEvent()
        initExchangeRateLiveData()
        initAllDisplayOrdersLiveData()
        initCurrentExchangeCoinLiveDataLiveData()
        handleMarkSocket()
    }

    private fun initCurrentExchangeCoinLiveDataLiveData() {
        val change = {
            val token = if (isPositiveChangeLiveData.value == true) {
                currentFormCoinLiveData.value
            } else {
                currentToCoinLiveData.value
            }
            currentExchangeCoinLiveData.postValue(token)
        }
        currentExchangeCoinLiveData.addSource(isPositiveChangeLiveData) {
            change()
        }
        currentExchangeCoinLiveData.addSource(currentToCoinLiveData) {
            change()
        }
        currentExchangeCoinLiveData.addSource(currentFormCoinLiveData) {
            change()
        }
    }

    fun getTokenList(): List<IToken> {
        return mTokenList.subList(0, mTokenList.size)
    }

    fun clickShowMoreAllOrder() {
        isShowMoreAllOrderLiveData.value = !isShowMoreAllOrderLiveData.value!!
    }

    private fun handleAccountEvent() =
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler()) {
            checkIsEnable()
            loadTokenList()
            resetMarkSocket()
        }

    private fun resetMarkSocket() {
        if (mAccount != null
            && currentFormCoinLiveData.value != null
            && currentToCoinLiveData.value != null
            && isPositiveChangeLiveData.value != null
        ) {
            val baseToken: String
            val tokenQuote: String
            if (isPositiveChangeLiveData.value == true) {
                baseToken = currentFormCoinLiveData.value!!.tokenAddress()
                tokenQuote = currentToCoinLiveData.value!!.tokenAddress()
            } else {
                baseToken = currentToCoinLiveData.value!!.tokenAddress()
                tokenQuote = currentFormCoinLiveData.value!!.tokenAddress()
            }
            ExchangeSocket.getMark(baseToken, tokenQuote, mAccount!!.address)
        }
    }

    @Subscribe
    fun onSwitchAccountEvent(event: SwitchAccountEvent) {
        handleAccountEvent()
    }

    /**
     * 检查当前账户
     */
    private suspend fun checkIsEnable() {
        mAccount = try {
            mAccountManager.currentAccount()
        } catch (e: Exception) {
            null
        }
        isEnable.postValue(mAccount?.coinNumber == CoinTypes.VToken.coinType())
    }

    private suspend fun loadTokenList() {
        try {
            mAccount?.let {
                val tokenPrices =
                    viewModelScope.async(Dispatchers.IO) {
                        DataRepository.getDexService().getTokenPrices()
                    }

                val localEnableToken =
                    viewModelScope.async(Dispatchers.IO) { TokenManager().loadEnableToken(it) }
                val remoteEnableToken =
                    viewModelScope.async(Dispatchers.IO) {
                        DataRepository.getViolasService().getRegisterToken(it.address)
                    }

                val localEnableTokenSet =
                    localEnableToken.await().map { it.tokenAddress }.toHashSet()
                val remoteEnableTokenSet = remoteEnableToken.await()?.toHashSet()
                // todo network 异常
                mTokenList.clear()
                tokenPrices.await()?.forEach {
                    val address = it.address.replace("0x", "")
                    val localEnable = localEnableTokenSet.contains(address)
                    val remoteEnable = remoteEnableTokenSet?.contains(address) ?: false
                    mTokenList.add(
                        ExchangeToken(
                            address,
                            it.name,
                            BigDecimal(it.price.toString()),
                            localEnable,
                            remoteEnable
                        )
                    )
                }
                if (mTokenList.size > 0) {
                    currentFormCoinLiveData.postValue(mTokenList[0])
                    currentToCoinLiveData.postValue(mTokenList[0])
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initExchangeRateLiveData() {
        exchangeRateLiveData.addSource(exchangeRate) {
            handleExchangeRateLiveData()
        }
        exchangeRateLiveData.addSource(isPositiveChangeLiveData) {
            handleExchangeRateLiveData()
        }
        exchangeRateLiveData.addSource(currentFormCoinLiveData) {
            handleExchangeRateLiveData()
        }
        exchangeRateLiveData.addSource(currentToCoinLiveData) {
            handleExchangeRateLiveData()
        }
    }

    private fun initAllDisplayOrdersLiveData() {
        allDisplayOrdersLiveData.addSource(isShowMoreAllOrderLiveData) {
            handleDisplayOrder()
        }
        allDisplayOrdersLiveData.addSource(allOrdersLiveData) {
            handleDisplayOrder()
        }
    }

    private fun handleDisplayOrder() {
        val take = allOrdersLiveData.value?.sortedBy { it.version() }?.take(
            if (isShowMoreAllOrderLiveData.value == true) {
                mShowMoreAllOrderMaxCount
            } else {
                mShowMoreAllOrderMinCount
            }
        )
        allDisplayOrdersLiveData.postValue(take)
    }

    private fun handleExchangeRateLiveData() {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler()) {
            if (currentFormCoinLiveData.value != null && currentToCoinLiveData.value != null && isPositiveChangeLiveData.value != null) {
                val fromUnit: String
                val fromPrice: BigDecimal
                val toUnit: String
                val toPrice: BigDecimal
                if (isPositiveChangeLiveData.value == true) {
                    fromUnit = currentFormCoinLiveData.value!!.tokenUnit()
                    fromPrice = currentFormCoinLiveData.value!!.tokenPrice()
                    toUnit = currentToCoinLiveData.value!!.tokenUnit()
                    toPrice = currentToCoinLiveData.value!!.tokenPrice()
                } else {
                    fromUnit = currentToCoinLiveData.value!!.tokenUnit()
                    fromPrice = currentToCoinLiveData.value!!.tokenPrice()
                    toUnit = currentFormCoinLiveData.value!!.tokenUnit()
                    toPrice = currentFormCoinLiveData.value!!.tokenPrice()
                }
                val divide = toPrice.divide(
                    fromPrice,
                    2,
                    RoundingMode.HALF_DOWN
                )
                exchangeRateNumberLiveData.postValue(divide)
                exchangeRateLiveData.postValue(
                    "1 $toUnit = ${divide.stripTrailingZeros().toPlainString()} $fromUnit"
                )
            } else {
                exchangeRateNumberLiveData.postValue(BigDecimal("0"))
                exchangeRateLiveData.postValue("... = ...")
            }
        }
    }

    private fun handleMarkSocket() {
        currentFormCoinLiveData.observeForever {
            resetMarkSocket()
        }
        currentToCoinLiveData.observeForever {
            resetMarkSocket()
        }
        isPositiveChangeLiveData.observeForever {
            resetMarkSocket()
        }
    }

    fun clickPositiveChange() {
        isPositiveChangeLiveData.value = !isPositiveChangeLiveData.value!!
    }

    override fun onMarkCall(
        myOrder: List<IOrder>,
        buyOrder: List<IOrder>,
        sellOrder: List<IOrder>
    ) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler()) {
            meOrdersLiveData.postValue(myOrder.map(setOrderPrice()))
            val allOrderList = buyOrder.plus(sellOrder)
            allOrdersLiveData.postValue(allOrderList.map(setOrderPrice()))
        }
    }

    override fun onDepthsCall(
        buyOrder: List<IOrder>,
        sellOrder: List<IOrder>
    ) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler()) {
            val allOrderList = buyOrder.plus(sellOrder)
            val meOrderList = allOrderList
                .filter {
                    mAccount?.address == it.userAddress()
                }
                .toList()
            if (meOrderList.isNotEmpty()) {
                val newMeOrderList =
                    meOrdersLiveData.value?.toMutableMap { it.id() } ?: mutableMapOf()
                meOrderList.forEach {
                    newMeOrderList[it.id()] = it
                }
                meOrdersLiveData.postValue(
                    newMeOrderList.values.toList()
                        .sortedBy { it.version() }
                        .map(setOrderPrice())
                        .take(3)
                )
            }
            val newAllOrderList =
                allOrdersLiveData.value?.toMutableMap { it.id() } ?: mutableMapOf()
            allOrderList.forEach {
                newAllOrderList[it.id()] = it
            }
            allOrdersLiveData.postValue(newAllOrderList.values.toList().map(setOrderPrice()))
        }
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        ExchangeSocket.removeSubscriber(this)
        super.onCleared()
    }

    private fun setOrderPrice(): (IOrder) -> IOrder = {
        currentToCoinLiveData.value?.let { token ->
            it.setPrice(
                token.tokenPrice()
//                    .divide(
//                        BigDecimal("100"),
//                        2,
//                        RoundingMode.HALF_DOWN
//                    )
                    .stripTrailingZeros().toPlainString()
            )
        }
        it
    }

    fun changeToCoinAmount(get: String?) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler()) {
            val amount =
                if (currentExchangeCoinLiveData.value != null && get != null && get.isNotEmpty()) {
                    BigDecimal(get)
                        .multiply(exchangeRateNumberLiveData.value)
                } else {
                    BigDecimal("0")
                }
            if (amount == BigDecimal("0")) {
                mToCoinAmountLiveData.postValue("")
            } else {
                mToCoinAmountLiveData.postValue(amount.stripTrailingZeros().toPlainString())
            }
        }
    }

    fun changeFromCoinAmount(get: String?) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler()) {
            val amount =
                if (currentExchangeCoinLiveData.value != null && get != null && get.isNotEmpty()) {
                    BigDecimal(get)
                        .divide(
                            exchangeRateNumberLiveData.value!!,
                            2,
                            BigDecimal.ROUND_HALF_UP
                        )
                } else {
                    BigDecimal("0")
                }
            if (amount == BigDecimal("0")) {
                mFromCoinAmountLiveData.postValue("")
            } else {
                mFromCoinAmountLiveData.postValue(amount.stripTrailingZeros().toPlainString())
            }
        }
    }

    private fun getCurrentUnPublishToken(): List<IToken> {
        val notEnable = mutableListOf<IToken>()
        if (currentFormCoinLiveData.value?.isNetEnable() == false) {
            notEnable.add(currentFormCoinLiveData.value!!)
        }
        if (currentToCoinLiveData.value?.isNetEnable() == false) {
            notEnable.add(currentToCoinLiveData.value!!)
        }
        return notEnable
    }

    @Throws(ExchangeCoinEqualException::class, WrongPasswordException::class)
    suspend fun handleExchange(
        password: ByteArray,
        fromCoinAmount: BigDecimal,
        toCoinAmount: BigDecimal
    ) = withContext(Dispatchers.IO) {
        if (currentFormCoinLiveData.value != null && currentFormCoinLiveData.value == currentToCoinLiveData.value) {
            throw ExchangeCoinEqualException()
        }

        val decryptPrivateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(password, mAccount?.privateKey)
            ?: throw WrongPasswordException()

        val account = Account(KeyPair.fromSecretKey(decryptPrivateKey))
        val currentUnPublishToken = getCurrentUnPublishToken()

        // publish
        val list = arrayListOf<Deferred<*>>()
        currentUnPublishToken.forEach {
            list.add(async {
                val publishToken = publishToken(account, it.tokenAddress())
                if (publishToken) {
                    it.setNetEnable(true)
                }
                publishToken
            })
        }
        list.forEach {
            it.await()
        }

        // exchange
        val fromCoin: IToken
        val toCoin: IToken

        if (isPositiveChangeLiveData.value == false) {
            fromCoin = currentFormCoinLiveData.value!!
            toCoin = currentToCoinLiveData.value!!
        } else {
            fromCoin = currentToCoinLiveData.value!!
            toCoin = currentFormCoinLiveData.value!!
        }

        Log.e("==exchange==", "${fromCoin.tokenName()}   ${toCoin.tokenName()}")
        mExchangeManager.exchangeToken(
            getApplication(),
            account,
            fromCoin,
            fromCoinAmount,
            toCoin,
            toCoinAmount
        )
    }

    private suspend fun publishToken(mAccount: Account, tokenAddress: String): Boolean {
        val channel = Channel<Boolean>()
        DataRepository.getViolasService()
            .publishToken(
                getApplication(),
                mAccount,
                tokenAddress
            ) {
                viewModelScope.launch(Dispatchers.IO) {
                    channel.send(it)
                    channel.close()
                }
            }
        return channel.receive()
        return true
    }
}

class ExchangeCoinEqualException : RuntimeException()