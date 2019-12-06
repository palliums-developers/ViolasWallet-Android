package com.violas.wallet.ui.main.quotes

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.violas.wallet.repository.socket.ExchangeSocket
import com.violas.wallet.repository.socket.Subscriber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

interface IToken {
    fun isNetEnable(): Boolean
    fun isEnable(): Boolean
    fun tokenAddress(): String
    fun tokenName(): String
    fun tokenUnit(): String
    fun tokenPrice(): BigDecimal
}

interface IOrder {
    fun version(): Long
}

class QuotesViewModel(application: Application) : AndroidViewModel(application), Subscriber {
    // 当前选择的币种
    val currentFormCoinLiveData = MutableLiveData<IToken>()
    val currentToCoinLiveData = MutableLiveData<IToken>()
    // 是否是正兑换
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

    init {
        ExchangeSocket.addSubscriber(this)

        initExchangeRateLiveData()
        initAllDisplayOrdersLiveData()
        ExchangeSocket.getMark("")
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
        viewModelScope.launch(Dispatchers.IO) {
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
                    "1 $fromUnit = ${toPrice.divide(
                        fromPrice,
                        2,
                        RoundingMode.HALF_DOWN
                    ).stripTrailingZeros().toPlainString()} $toUnit"
                )
            } else {
                exchangeRateLiveData.postValue("... = ...")
            }
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
}