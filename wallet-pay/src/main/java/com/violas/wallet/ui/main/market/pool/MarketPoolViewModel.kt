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
import com.violas.wallet.utils.convertAmountToDisplayAmount
import com.violas.wallet.utils.convertAmountToExchangeRate
import com.violas.wallet.utils.convertDisplayAmountToAmount
import kotlinx.coroutines.*
import java.math.BigDecimal

/**
 * Created by elephant on 2020/6/30 17:42.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MarketPoolViewModel : BaseViewModel() {

    companion object {
        /**
         * 获取用户可转出的流动性token列表
         */
        const val ACTION_GET_USER_LIQUIDITY_TOKENS = 0x01

        /**
         * 添加流动性估算
         */
        const val ACTION_ADD_LIQUIDITY_ESTIMATE = 0x02

        /**
         * 移除流动性估算
         */
        const val ACTION_REMOVE_LIQUIDITY_ESTIMATE = 0x03

        /**
         * 添加流动性
         */
        const val ACTION_ADD_LIQUIDITY = 0x04

        /**
         * 移除流动性
         */
        const val ACTION_REMOVE_LIQUIDITY = 0x05
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

            exchangeRateLiveData.postValue(null)

            firstInputTextLiveData.postValue("")
            secondInputTextLiveData.postValue("")
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
    fun getFirstInputTextLiveData(): MutableLiveData<String> {
        return firstInputTextLiveData
    }

    fun getSecondInputTextLiveData(): MutableLiveData<String> {
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

                /*val liquidityAmount = BigDecimal(inputLiquidityAmount)
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
                )*/
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
        when (action) {
            ACTION_GET_USER_LIQUIDITY_TOKENS -> {
                val userPoolInfo =
                    exchangeManager.mViolasService.getUserPoolInfo(address!!)
                liquidityTokensLiveData.postValue(userPoolInfo?.liquidityTokens)
            }

            ACTION_ADD_LIQUIDITY_ESTIMATE -> {
                val inputAmount = params[0] as String
                val tokenAName = params[1] as String
                val tokenBName = params[2] as String

                val tokenAAmount = convertDisplayAmountToAmount(inputAmount)
                val result =
                    exchangeManager.mViolasService.addPoolLiquidityEstimate(
                        tokenAName = tokenAName,
                        tokenBName = tokenBName,
                        tokenAAmount = tokenAAmount.toPlainString()
                    )

                if (currFirstTokenLiveData.value!!.module == tokenAName) {
                    secondInputTextLiveData.postValue(
                        convertAmountToDisplayAmount(result.tokenBAmount)
                    )
                    exchangeRateLiveData.postValue(
                        convertAmountToExchangeRate(tokenAAmount, result.tokenBAmount)
                    )
                } else {
                    firstInputTextLiveData.postValue(
                        convertAmountToDisplayAmount(result.tokenBAmount)
                    )
                    exchangeRateLiveData.postValue(
                        convertAmountToExchangeRate(result.tokenBAmount, tokenAAmount)
                    )
                }
            }

            ACTION_REMOVE_LIQUIDITY_ESTIMATE -> {
                val inputAmount = params[0] as String
                val tokenAName = params[1] as String
                val tokenBName = params[2] as String

                val result =
                    exchangeManager.mViolasService.removePoolLiquidityEstimate(
                        address = address!!,
                        tokenAName = tokenAName,
                        tokenBName = tokenBName,
                        liquidityAmount = convertDisplayAmountToAmount(inputAmount).toPlainString()
                    )

                val displayAmountA =
                    convertAmountToDisplayAmount(result.tokenAAmount) + result.tokenAName
                val displayAmountB =
                    convertAmountToDisplayAmount(result.tokenBAmount) + result.tokenBName
                secondInputTextLiveData.postValue("$displayAmountA\n$displayAmountB")

                exchangeRateLiveData.postValue(
                    convertAmountToExchangeRate(result.tokenAAmount, result.tokenBAmount)
                )
            }

            ACTION_ADD_LIQUIDITY -> {

            }

            ACTION_REMOVE_LIQUIDITY -> {

            }

            else -> {
                error("Unsupported action: $action")
            }
        }
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        if (action == ACTION_GET_USER_LIQUIDITY_TOKENS && address.isNullOrBlank()) {
            tipsMessage.postValueSupport(getString(R.string.tips_create_or_import_wallet))
            return false
        }
        return super.checkParams(action, *params)
    }
}