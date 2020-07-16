package com.violas.wallet.ui.main.market.pool

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.utils.getString
import com.palliums.violas.http.LiquidityTokenDTO
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ExchangeManager
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import kotlinx.coroutines.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Created by elephant on 2020/6/30 17:42.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MarketPoolViewModel : BaseViewModel() {

    companion object {
        const val ACTION_GET_TOKEN_PAIRS = 0x01
    }

    // 当前的操作模式，分转入和转出
    private val currOpModeLiveData = MutableLiveData<MarketPoolOpMode>(MarketPoolOpMode.TransferIn)

    // 转入模式下选择的Token
    private val currFirstTokenLiveData = MediatorLiveData<StableTokenVo?>()
    private val currSecondTokenLiveData = MediatorLiveData<StableTokenVo?>()

    // 转出模式下选择的交易对和可转出的交易对列表
    private val currLiquidityTokenLiveData = MediatorLiveData<LiquidityTokenDTO?>()
    private val liquidityTokensLiveData = MutableLiveData<List<LiquidityTokenDTO>?>()

    // 兑换率
    private val exchangeRateLiveData = MediatorLiveData<BigDecimal?>()

    // 资金池通证及占比
    private val poolTokenAndPoolShareLiveData = MediatorLiveData<Pair<String, String>?>()

    // 输入框文本变化
    private val firstInputTextLiveData = MutableLiveData<String>()
    private val secondInputTextLiveData = MutableLiveData<String>()

    private val exchangeManager by lazy { ExchangeManager() }
    private var address: String? = null
    private var calculateAmountJob: Job? = null

    init {
        currFirstTokenLiveData.addSource(currOpModeLiveData) {
            currFirstTokenLiveData.postValue(null)
            currSecondTokenLiveData.postValue(null)
            currLiquidityTokenLiveData.postValue(null)
        }

        val convertToExchangeRate: (String, String) -> BigDecimal? =
            { amountAStr, amountBStr ->
                val amountA = BigDecimal(amountAStr)
                val amountB = BigDecimal(amountBStr)
                val zero = BigDecimal("0.00")
                if (amountA <= zero || amountB <= zero)
                    null
                else
                    amountB.divide(amountA, 8, RoundingMode.DOWN)
                        .stripTrailingZeros()
            }
        exchangeRateLiveData.addSource(currFirstTokenLiveData) { firstToken ->
            val secondToken = currSecondTokenLiveData.value
            exchangeRateLiveData.postValue(
                if (firstToken == null || secondToken == null)
                    null
                else
                    convertToExchangeRate(
                        firstToken.anchorValue.toString(),
                        secondToken.anchorValue.toString()
                    )
            )
        }
        exchangeRateLiveData.addSource(currSecondTokenLiveData) { secondToken ->
            val firstToken = currFirstTokenLiveData.value
            exchangeRateLiveData.postValue(
                if (firstToken == null || secondToken == null)
                    null
                else
                    convertToExchangeRate(
                        firstToken.anchorValue.toString(),
                        secondToken.anchorValue.toString()
                    )
            )
        }
        exchangeRateLiveData.addSource(currLiquidityTokenLiveData) {
            exchangeRateLiveData.postValue(
                if (it == null)
                    null
                else
                    convertToExchangeRate(it.coinAAmount, it.coinBAmount)
            )
        }
    }

    //*********************************** 操作模式相关方法 ***********************************//
    fun getCurrOpModeLiveData(): LiveData<MarketPoolOpMode> {
        return currOpModeLiveData
    }

    fun getCurrOpModelPosition(): Int {
        return currOpModeLiveData.value?.ordinal ?: -1
    }

    fun switchOpModel(target: MarketPoolOpMode) {
        if (target != currOpModeLiveData.value) {
            currOpModeLiveData.postValue(target)

            // 切换操作模式时，清除second input box的文本
            cancelCalculateAmountJob()
            secondInputTextLiveData.postValue("")
        }
    }

    fun isTransferInMode(): Boolean {
        return currOpModeLiveData.value == MarketPoolOpMode.TransferIn
    }

    //*********************************** 转入模式下相关方法 ***********************************//
    fun getCurrFirstTokenLiveData(): LiveData<StableTokenVo?> {
        return currFirstTokenLiveData
    }

    fun getCurrSecondTokenLiveData(): LiveData<StableTokenVo?> {
        return currSecondTokenLiveData
    }

    fun selectToken(selectFirst: Boolean, selected: StableTokenVo) {
        if (selectFirst) {
            val currFirstToken = currFirstTokenLiveData.value
            if (selected != currFirstToken) {
                val currSecondToken = currSecondTokenLiveData.value
                if (selected == currSecondToken) {
                    currSecondTokenLiveData.postValue(currFirstToken)
                }
                currFirstTokenLiveData.postValue(selected)
            }
        } else {
            val currSecondToken = currSecondTokenLiveData.value
            if (selected != currSecondToken) {
                val currFirstToken = currFirstTokenLiveData.value
                if (selected == currFirstToken) {
                    currFirstTokenLiveData.postValue(currSecondToken)
                }
                currSecondTokenLiveData.postValue(selected)
            }
        }
    }

    //*********************************** 转出模式下相关方法 ***********************************//
    fun getCurrLiquidityTokenLiveData(): LiveData<LiquidityTokenDTO?> {
        return currLiquidityTokenLiveData
    }

    fun getLiquidityTokensLiveData(): MutableLiveData<List<LiquidityTokenDTO>?> {
        return liquidityTokensLiveData
    }

    fun getCurrLiquidityTokenPosition(): Int {
        val curr = currLiquidityTokenLiveData.value ?: return -1
        val list = liquidityTokensLiveData.value ?: return -1
        list.forEachIndexed { index, item ->
            if (curr.coinAIndex == item.coinAIndex && curr.coinBIndex == item.coinBIndex) {
                return index
            }
        }
        return -1
    }

    fun selectLiquidityToken(
        selectedPosition: Int,
        currPosition: Int = getCurrLiquidityTokenPosition()
    ) {
        if (selectedPosition != currPosition) {
            val list = liquidityTokensLiveData.value ?: return
            if (selectedPosition < 0 || selectedPosition >= list.size) return
            currLiquidityTokenLiveData.postValue(list[selectedPosition])
        }
    }

    //*********************************** 其它信息相关方法 ***********************************//
    fun getExchangeRateLiveData(): LiveData<BigDecimal?> {
        return exchangeRateLiveData
    }

    fun getPoolTokenAndPoolShareLiveData(): LiveData<Pair<String, String>?> {
        return poolTokenAndPoolShareLiveData
    }

    //*********************************** 输入金额联动方法 ***********************************//
    fun getFirstInputTextLiveData(): LiveData<String> {
        return firstInputTextLiveData
    }

    fun getSecondInputTextLiveData(): LiveData<String> {
        return secondInputTextLiveData
    }

    fun changeFirstTokenTransferIntoAmount(secondInputAmount: String?) {
        val firstToken = currFirstTokenLiveData.value ?: return
        val secondToken = currSecondTokenLiveData.value ?: return
        cancelCalculateAmountJob()
        calculateAmountJob = viewModelScope.launch {
            delay(100)
            withContext(Dispatchers.IO) {
                // TODO
                if (secondInputAmount.isNullOrBlank()) {
                    secondInputTextLiveData.postValue("0.00")
                    return@withContext
                }
            }

            calculateAmountJob = null
        }
    }

    fun changeSecondTokenTransferIntoAmount(firstInputAmount: String?) {
        val firstToken = currFirstTokenLiveData.value ?: return
        val secondToken = currSecondTokenLiveData.value ?: return
        cancelCalculateAmountJob()
        calculateAmountJob = viewModelScope.launch {
            delay(100)
            withContext(Dispatchers.IO) {
                // TODO
                if (firstInputAmount.isNullOrBlank()) {
                    secondInputTextLiveData.postValue("0.00")
                    return@withContext
                }
            }

            calculateAmountJob = null
        }
    }

    fun changeTokensTransferOutAmount(inputLiquidityAmount: String?) {
        val liquidityToken = currLiquidityTokenLiveData.value ?: return
        cancelCalculateAmountJob()
        calculateAmountJob = viewModelScope.launch {
            delay(100)
            withContext(Dispatchers.IO) {
                if (inputLiquidityAmount.isNullOrBlank()) {
                    secondInputTextLiveData.postValue("")
                    return@withContext
                }

                val liquidityAmount = BigDecimal(inputLiquidityAmount)
                val tokenATransferOutAmount = liquidityAmount
                    .multiply(BigDecimal(liquidityToken.coinAAmount))
                    .divide(BigDecimal(liquidityToken.amount), 6, RoundingMode.DOWN)
                    .stripTrailingZeros().toPlainString() + liquidityToken.coinAName
                val tokenBTransferOutAmount = liquidityAmount
                    .multiply(BigDecimal(liquidityToken.coinBAmount))
                    .divide(BigDecimal(liquidityToken.amount), 6, RoundingMode.DOWN)
                    .stripTrailingZeros().toPlainString() + liquidityToken.coinBName
                secondInputTextLiveData.postValue(
                    "$tokenATransferOutAmount\n$tokenBTransferOutAmount"
                )
            }

            calculateAmountJob = null
        }
    }

    private fun cancelCalculateAmountJob() {
        calculateAmountJob?.let {
            it.cancel()
            calculateAmountJob = null
        }
    }

    //*********************************** 耗时相关任务 ***********************************//
    fun setAddress(existsAccount: Boolean): Job {
        return viewModelScope.launch {
            if (!existsAccount) {
                address = null
                return@launch
            }

            val violasAccount = withContext(Dispatchers.IO) {
                AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
            } ?: return@launch
            address = violasAccount.address
            address = "fa279f2615270daed6061313a48360f7"
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        // 获取可转出的流动性token列表
        if (action == ACTION_GET_TOKEN_PAIRS) {
            val userPoolInfo =
                exchangeManager.mViolasService.getUserPoolInfo(address!!)
            liquidityTokensLiveData.postValue(userPoolInfo?.liquidityTokens)
            return
        }

        // 转入
        if (isTransferInMode()) {
            val firstToken = currFirstTokenLiveData.value!!
            val secondToken = currSecondTokenLiveData.value!!
            return
        }

        // 转出
        val liquidityToken = currLiquidityTokenLiveData.value!!
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        if (action == ACTION_GET_TOKEN_PAIRS && address.isNullOrBlank()) {
            tipsMessage.postValueSupport(getString(R.string.tips_create_or_import_wallet))
            return false
        }
        return super.checkParams(action, *params)
    }
}