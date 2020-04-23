package com.violas.wallet.ui.main.quotes

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.content.ContextProvider
import com.palliums.utils.exceptionAsync
import com.palliums.utils.coroutineExceptionHandler
import com.palliums.utils.getString
import com.palliums.utils.toMutableMap
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.*
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.event.RefreshBalanceEvent
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.event.TokenPublishEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.socket.ExchangeSocket
import com.violas.wallet.repository.socket.Subscriber
import com.violas.wallet.ui.main.quotes.bean.ExchangeToken
import com.violas.wallet.ui.main.quotes.bean.IOrder
import com.violas.wallet.ui.main.quotes.bean.IOrderStatus
import com.violas.wallet.ui.main.quotes.bean.IToken
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.palliums.violascore.crypto.KeyPair
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal
import java.math.RoundingMode

class QuotesViewModel(application: Application) : AndroidViewModel(application), Subscriber,
    Handler.Callback {
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
    val mFromCoinAmountLiveData = MutableLiveData<BigDecimal>()
    val mToCoinAmountLiveData = MutableLiveData<BigDecimal>()

    // Token 列表
    private val mTokenList = ArrayList<IToken>()

    // 加载进度条状态
    val mLoadingLiveData = MutableLiveData<Boolean>(true)

    private var oldBaseToken: String? = null
    private var oldTokenQuote: String? = null

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

    private var mDefToken = object : IToken {
        override fun setNetEnable(enable: Boolean) {

        }

        override fun isNetEnable(): Boolean {
            return true
        }

        override fun tokenIdx(): Long {
            return 0
        }

        override fun tokenName(): String {
            return getString(R.string.hinet_token_select)
        }

        override fun tokenPrice(): BigDecimal {
            return BigDecimal("1")
        }
    }

    // 延时任务
    private val mHandler = Handler(Looper.getMainLooper(), this)
    private val mDelayRefreshMark = 0x001
    private val mDelayRefreshMarkTime = 20 * 1000L

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
            val token = if (isPositiveChangeLiveData.value == false) {
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

    suspend fun getTokenList(fromCoin: Boolean) = withContext(Dispatchers.IO) {
        val selectCoin = if (fromCoin) {
            currentToCoinLiveData.value
        } else {
            currentFormCoinLiveData.value
        }

        mTokenList.subList(0, mTokenList.size).filter {
            it.tokenIdx() != selectCoin?.tokenIdx()
        }
    }

    fun clickShowMoreAllOrder() {
        isShowMoreAllOrderLiveData.value = !isShowMoreAllOrderLiveData.value!!
    }

    fun changeFromCoin(token: IToken) {
        if (currentFormCoinLiveData.value?.tokenIdx() != token.tokenIdx()) {
            currentFormCoinLiveData.postValue(token)
        }
    }

    fun changeToCoin(token: IToken) {
        if (currentToCoinLiveData.value?.tokenIdx() != token.tokenIdx()) {
            currentToCoinLiveData.postValue(token)
        }
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
            && isEnable.value == true
        ) {
            mHandler.removeMessages(mDelayRefreshMark)
            exchangeRateNumberLiveData.postValue(BigDecimal("0"))
            mLoadingLiveData.postValue(true)
            allDisplayOrdersLiveData.postValue(listOf())
            allOrdersLiveData.postValue(listOf())

            val baseToken: String
            val tokenQuote: String
            if (isPositiveChangeLiveData.value == true) {
                baseToken = currentFormCoinLiveData.value!!.tokenIdx().toString()
                tokenQuote = currentToCoinLiveData.value!!.tokenIdx().toString()
            } else {
                baseToken = currentToCoinLiveData.value!!.tokenIdx().toString()
                tokenQuote = currentFormCoinLiveData.value!!.tokenIdx().toString()
            }
            ExchangeSocket.unSubscribe(oldBaseToken, oldTokenQuote)
            oldBaseToken = baseToken
            oldTokenQuote = tokenQuote
            ExchangeSocket.getMark(baseToken, tokenQuote, mAccount!!.address)

            mHandler.sendMessageDelayed(
                Message.obtain(mHandler, mDelayRefreshMark),
                mDelayRefreshMarkTime
            )
        }
    }

    @Subscribe
    fun onSwitchAccountEvent(event: SwitchAccountEvent) {
        handleAccountEvent()
    }

    @Subscribe
    fun onTokenPublishEvent(event: TokenPublishEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            loadTokenList()
        }
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
        val enable = mAccount?.coinNumber == CoinTypes.Violas.coinType()
        withContext(Dispatchers.Main) {
            isEnable.value = enable
        }
    }

    private suspend fun loadTokenList() {
        if (isEnable.value == false) {
            return
        }
        mAccount?.let {
            coroutineScope {
                try {
                    val tokenPrices = exceptionAsync { DataRepository.getDexService().getTokens() }
                    val localEnableToken = exceptionAsync { TokenManager().loadEnableToken(it) }

                    val localEnableTokenSet =
                        localEnableToken.await().map { it.tokenIdx }.toHashSet()
                    // todo network 异常
                    mTokenList.clear()
                    tokenPrices.await().forEach {
                        val tokenIdx = it.id
                        val localEnable = localEnableTokenSet.contains(it.id.toLong())
                        mTokenList.add(
                            ExchangeToken(
                                tokenIdx.toLong(),
                                it.name,
                                localEnable,
                                true
                            )
                        )
                    }
                    if (mTokenList.size > 0) {
                        currentFormCoinLiveData.postValue(mTokenList[0])
                    } else {
                        currentFormCoinLiveData.postValue(mDefToken)
                    }
                    if (mTokenList.size > 1) {
                        currentToCoinLiveData.postValue(mTokenList[1])
                    } else {
                        currentToCoinLiveData.postValue(mDefToken)
                    }
                } catch (e: Exception) {
//                    e.printStackTrace()
                    currentFormCoinLiveData.postValue(mDefToken)
                    currentToCoinLiveData.postValue(mDefToken)
                }
            }
        }
    }

    private fun initExchangeRateLiveData() {
//        exchangeRateLiveData.addSource(exchangeRate) {
//            handleExchangeRateLiveData()
//        }
//        exchangeRateLiveData.addSource(isPositiveChangeLiveData) {
//            handleExchangeRateLiveData()
//        }
//        exchangeRateLiveData.addSource(currentFormCoinLiveData) {
//            handleExchangeRateLiveData()
//        }
//        exchangeRateLiveData.addSource(currentToCoinLiveData) {
//            handleExchangeRateLiveData()
//        }
        exchangeRateLiveData.addSource(exchangeRateNumberLiveData) {
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
        val take = allOrdersLiveData.value?.take(
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
            if (currentFormCoinLiveData.value != null
                && currentToCoinLiveData.value != null
                && isPositiveChangeLiveData.value != null
                && exchangeRateNumberLiveData.value != null
                && exchangeRateNumberLiveData.value != BigDecimal("0")
            ) {
                val fromUnit: String
                val toUnit: String
                if (isPositiveChangeLiveData.value == false) {
                    fromUnit = currentFormCoinLiveData.value!!.tokenUnit()
                    toUnit = currentToCoinLiveData.value!!.tokenUnit()
                } else {
                    fromUnit = currentToCoinLiveData.value!!.tokenUnit()
                    toUnit = currentFormCoinLiveData.value!!.tokenUnit()
                }
                exchangeRateLiveData.postValue(
                    "1 $toUnit ≈ ${exchangeRateNumberLiveData.value!!.stripTrailingZeros()
                        .toPlainString()} $fromUnit"
                )
            } else {
                //exchangeRateNumberLiveData.postValue(BigDecimal("0"))
                exchangeRateLiveData.postValue("... ≈ ...")
            }
        }
//        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler()) {
//            if (currentFormCoinLiveData.value != null && currentToCoinLiveData.value != null && isPositiveChangeLiveData.value != null) {
//                val fromUnit: String
//                val fromPrice: BigDecimal
//                val toUnit: String
//                val toPrice: BigDecimal
//                if (isPositiveChangeLiveData.value == false) {
//                    fromUnit = currentFormCoinLiveData.value!!.tokenUnit()
//                    fromPrice = currentFormCoinLiveData.value!!.tokenPrice()
//                    toUnit = currentToCoinLiveData.value!!.tokenUnit()
//                    toPrice = currentToCoinLiveData.value!!.tokenPrice()
//                } else {
//                    fromUnit = currentToCoinLiveData.value!!.tokenUnit()
//                    fromPrice = currentToCoinLiveData.value!!.tokenPrice()
//                    toUnit = currentFormCoinLiveData.value!!.tokenUnit()
//                    toPrice = currentFormCoinLiveData.value!!.tokenPrice()
//                }
//                val divide = toPrice.divide(
//                    fromPrice,
//                    20,
//                    RoundingMode.DOWN
//                )
//                exchangeRateNumberLiveData.postValue(divide)
//                exchangeRateLiveData.postValue(
//                    "1 $toUnit ≈ ${divide.setScale(
//                        4,
//                        RoundingMode.DOWN
//                    ).stripTrailingZeros().toPlainString()} $fromUnit"
//                )
//            } else {
//                exchangeRateNumberLiveData.postValue(BigDecimal("0"))
//                exchangeRateLiveData.postValue("... ≈ ...")
//            }
//        }
    }

    private fun handleMarkSocket() {
        isEnable.observeForever {
            if (it) {
                mLoadingLiveData.postValue(true)
                ExchangeSocket.addSubscriber(this)
            } else {
                mLoadingLiveData.postValue(false)
//                ExchangeSocket.unSubscribe(oldBaseToken, oldTokenQuote)
                ExchangeSocket.removeSubscriber(this)
            }
        }
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

    private fun orderFilter(order: IOrder): Boolean {
        return if (isPositiveChangeLiveData.value == true) {
            order.tokenGet() == currentToCoinLiveData.value?.tokenIdx().toString() &&
                    order.tokenGive() == currentFormCoinLiveData.value?.tokenIdx().toString()
        } else {
            order.tokenGet() == currentFormCoinLiveData.value?.tokenIdx().toString() &&
                    order.tokenGive() == currentToCoinLiveData.value?.tokenIdx().toString()
        }
    }

    override fun onMarkCall(
        myOrder: List<IOrder>,
        buyOrder: List<IOrder>,
        sellOrder: List<IOrder>,
        rate: BigDecimal
    ) {
        mLoadingLiveData.postValue(false)
        mHandler.removeMessages(mDelayRefreshMark)
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler()) {
            meOrdersLiveData.postValue(
                myOrder.map(setOrderPrice())
                    .sortedByDescending { it.version() }
            )
            val allOrderList = buyOrder//.plus(sellOrder)
                .filter { orderFilter(it) }
                .filter { it.userAddress() != mAccount?.address }
            allOrdersLiveData.postValue(
                allOrderList.map(setOrderPrice())
                    .sortedByDescending { it.version() }
            )
            exchangeRateNumberLiveData.postValue(rate)
        }
    }

    override fun onDepthsCall(
        buyOrder: List<IOrder>,
        sellOrder: List<IOrder>
    ) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler()) {
            val allOrderList = buyOrder//.plus(sellOrder)
                .filter { orderFilter(it) }

            val meOrderList = allOrderList
                .filter {
                    mAccount?.address == it.userAddress()
                }
                .toList()
            if (meOrderList.isNotEmpty()) {
                val newMeOrderList =
                    (meOrdersLiveData.value?.toMutableMap { it.id() } ?: mutableMapOf())

                meOrderList.forEach {
                    when (it.state()) {
                        IOrderStatus.OPEN -> newMeOrderList[it.id()] = it
                        IOrderStatus.FILLED,
                        IOrderStatus.CANCELED,
                        IOrderStatus.FILLED_CANCELED -> newMeOrderList.remove(it.id())
                    }
                }
                meOrdersLiveData.postValue(
                    newMeOrderList.values
                        .map(setOrderPrice())
                        .sortedByDescending { it.version() }
                )
            }
            val newAllOrderList =
                allOrdersLiveData.value?.toMutableMap { it.id() } ?: mutableMapOf()
            allOrderList.filter {
                it.userAddress() != mAccount?.address
            }.forEach {
                when (it.state()) {
                    IOrderStatus.OPEN -> newAllOrderList[it.id()] = it
                    IOrderStatus.FILLED,
                    IOrderStatus.CANCELED,
                    IOrderStatus.FILLED_CANCELED -> newAllOrderList.remove(it.id())
                }
            }
            allOrdersLiveData.postValue(
                newAllOrderList.values.toList()
                    .sortedByDescending { it.version() }
                    .take(20)
                    .map(setOrderPrice())
            )
        }
    }

    override fun onReconnect() {
        viewModelScope.launch(Dispatchers.Main + coroutineExceptionHandler()) {
            if (mAccount != null
                && currentFormCoinLiveData.value != null
                && currentToCoinLiveData.value != null
                && isPositiveChangeLiveData.value != null
            ) {
                val baseToken: String
                val tokenQuote: String
                if (isPositiveChangeLiveData.value == true) {
                    baseToken = currentFormCoinLiveData.value!!.tokenIdx().toString()
                    tokenQuote = currentToCoinLiveData.value!!.tokenIdx().toString()
                } else {
                    baseToken = currentToCoinLiveData.value!!.tokenIdx().toString()
                    tokenQuote = currentFormCoinLiveData.value!!.tokenIdx().toString()
                }
                oldBaseToken = baseToken
                oldTokenQuote = tokenQuote
                ExchangeSocket.getMark(baseToken, tokenQuote, mAccount!!.address)
            }
        }
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        ExchangeSocket.removeSubscriber(this)
        mHandler.removeCallbacksAndMessages(null)
        super.onCleared()
    }

    private fun setOrderPrice(): (IOrder) -> IOrder = {
        it.setPrice(
            it.tokenGetPrice()
                .setScale(
                    2,
                    RoundingMode.DOWN
                )
                .stripTrailingZeros().toPlainString()
        )
        it
    }

    fun changeToCoinAmount(get: String?) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler()) {
            val amount =
                if (currentExchangeCoinLiveData.value != null && get != null && get.isNotEmpty()) {
                    BigDecimal(get)
                        .multiply(exchangeRateNumberLiveData.value)
                        .setScale(2, RoundingMode.DOWN)
                } else {
                    BigDecimal("0")
                }
            mToCoinAmountLiveData.postValue(amount)
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
                            RoundingMode.UP
                        )
                } else {
                    BigDecimal("0")
                }
            mFromCoinAmountLiveData.postValue(amount)
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

    suspend fun handleCheckParam(
        fromCoinAmount: BigDecimal,
        toCoinAmount: BigDecimal
    ) = withContext(Dispatchers.IO) {
        if (currentFormCoinLiveData.value == mDefToken
            || currentToCoinLiveData.value == mDefToken
        ) {
            throw ExchangeNotSelectCoinException()
        }

        if (currentFormCoinLiveData.value != null && currentFormCoinLiveData.value == currentToCoinLiveData.value) {
            throw ExchangeCoinEqualException()
        }

        if (fromCoinAmount > BigDecimal("999999999999.99")
            || toCoinAmount > BigDecimal("999999999999.99")
        ) {
            throw ExchangeAmountLargeException()
        }

        val fromCoin: IToken
        val toCoin: IToken

        if (isPositiveChangeLiveData.value == true) {
            fromCoin = currentFormCoinLiveData.value!!
            toCoin = currentToCoinLiveData.value!!
        } else {
            fromCoin = currentToCoinLiveData.value!!
            toCoin = currentFormCoinLiveData.value!!
        }

        val tokenBalance =
            mTokenManager.getTokenBalance(mAccount!!.address, fromCoin.tokenIdx())

        if (fromCoinAmount.multiply(BigDecimal("1000000")).toLong() > tokenBalance) {
            throw LackOfBalanceException()
        }
    }

    @Throws(
        ExchangeCoinEqualException::class,
        WrongPasswordException::class,
        LackOfBalanceException::class,
        TransferUnknownException::class
    )
    suspend fun handleExchange(
        password: ByteArray,
        fromCoinAmount: BigDecimal,
        toCoinAmount: BigDecimal
    ) = withContext(Dispatchers.IO) {
        val decryptPrivateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(password, mAccount?.privateKey)
            ?: throw WrongPasswordException()

        val account = Account(KeyPair.fromSecretKey(decryptPrivateKey))
        val isPublish = mTokenManager.isPublish(account.getAddress().toHex())

        // exchange
        val fromCoin: IToken
        val toCoin: IToken

        if (isPositiveChangeLiveData.value == true) {
            fromCoin = currentFormCoinLiveData.value!!
            toCoin = currentToCoinLiveData.value!!
        } else {
            fromCoin = currentToCoinLiveData.value!!
            toCoin = currentFormCoinLiveData.value!!
        }

        if (!isPublish) {
            try {
                publishToken(account)
            } catch (e: Exception) {
                throw TransferUnknownException()
            }
        }

        Log.e("==exchange==", "${fromCoin.tokenName()}   ${toCoin.tokenName()}")
        val exchangeToken = mExchangeManager.exchangeToken(
            getApplication(),
            account,
            fromCoin,
            fromCoinAmount,
            toCoin,
            toCoinAmount
        )

        EventBus.getDefault().post(RefreshBalanceEvent())
        exchangeToken
    }

    private suspend fun publishToken(mAccount: Account): Boolean {
        mTokenManager.publishToken(mAccount)
        return true
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            mDelayRefreshMark -> resetMarkSocket()
        }
        return true
    }
}

class ExchangeAmountLargeException : RuntimeException()
class ExchangeCoinEqualException : RuntimeException()
class ExchangeNotSelectCoinException : RuntimeException()