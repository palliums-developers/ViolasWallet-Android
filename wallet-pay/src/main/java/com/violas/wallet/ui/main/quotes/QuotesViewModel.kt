package com.violas.wallet.ui.main.quotes

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.palliums.utils.coroutineExceptionHandler
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.socket.ExchangeSocket
import com.violas.wallet.repository.socket.Subscriber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

interface IToken {
    fun isNetEnable(): Boolean = false
    fun isEnable(): Boolean = false
    fun tokenAddress(): String
    fun tokenName(): String
    fun tokenUnit(): String = tokenName()
    fun tokenPrice(): BigDecimal
}

class ExchangeToken(
    private val address: String,
    private val name: String,
    private val price: BigDecimal,
    private var localEnable: Boolean = false,
    private var remoteEnable: Boolean = false
) : IToken {
    override fun tokenAddress() = address

    override fun tokenName() = name

    override fun tokenPrice() = price

    override fun isNetEnable() = remoteEnable

    override fun isEnable() = localEnable
}

interface IOrder {
    fun version(): Long
}

class QuotesViewModel(application: Application) : AndroidViewModel(application), Subscriber {
    // 是否开启兑换功能
    val isEnable = MutableLiveData(false)
    // 当前选择的币种
    val currentFormCoinLiveData = MutableLiveData<IToken>()
    val currentToCoinLiveData = MutableLiveData<IToken>()
    // 是否是正向兑换
    val isPositiveChangeLiveData = MutableLiveData(true)
    // 汇率
    val exchangeRateLiveData = MediatorLiveData<String>()
    private val exchangeRate = MutableLiveData("... = ...")
    // 当前委托
    val meOrdersLiveData = MutableLiveData<List<IOrder>>()
    // 他人委托
    val isShowMoreAllOrderLiveData = MutableLiveData(false)
    val allDisplayOrdersLiveData = MediatorLiveData<List<IOrder>>()
    private val allOrdersLiveData = MutableLiveData<List<IOrder>>()
    // Token 列表
    private val mTokenList = ArrayList<IToken>()

    private val mTokenManager by lazy {
        TokenManager()
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    private var mAccount: AccountDO? = null

    init {
        EventBus.getDefault().register(this)
        ExchangeSocket.addSubscriber(this)
        handleAccountEvent()
        initExchangeRateLiveData()
        initAllDisplayOrdersLiveData()
        handleMarkSocket()
    }

    fun getTokenList(): List<IToken> {
        return mTokenList.subList(0, mTokenList.size)
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
        allDisplayOrdersLiveData.addSource(isShowMoreAllOrderLiveData, Observer {

        })
        allDisplayOrdersLiveData.addSource(allOrdersLiveData, Observer {

        })
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
                exchangeRateLiveData.postValue(
                    "1 $toUnit = ${toPrice.divide(
                        fromPrice,
                        2,
                        RoundingMode.HALF_DOWN
                    ).stripTrailingZeros().toPlainString()} $fromUnit"
                )
            } else {
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

    override fun onMarkCall(msg: JSONObject) {
        Log.e("====", msg.toString())
        val mutableList = MutableList<IOrder>(10) {
            object : IOrder {
                override fun version(): Long {
                    return it.toLong()
                }
            }
        }
        meOrdersLiveData.postValue(mutableList)
        allDisplayOrdersLiveData.postValue(mutableList)
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        ExchangeSocket.removeSubscriber(this)
        super.onCleared()
    }
}