package com.violas.wallet.ui.main.market.pool

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.violas.http.LiquidityTokenDTO
import com.palliums.violas.http.MarketPairReserveInfoDTO
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ExchangeManager
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.wallet.utils.convertAmountToExchangeRate
import com.violas.wallet.utils.convertDisplayAmountToAmount
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
        /**
         * 获取用户可转出的流动性token列表
         */
        const val ACTION_GET_USER_LIQUIDITY_TOKENS = 0x01

        /**
         * 获取币种对储备信息
         */
        const val ACTION_GET_PAIR_RESERVE_INFO = 0x02

        /**
         * 添加流动性
         */
        const val ACTION_ADD_LIQUIDITY = 0x03

        /**
         * 移除流动性
         */
        const val ACTION_REMOVE_LIQUIDITY = 0x04
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
    private var violasAccountDO: AccountDO? = null
    private var estimateAmountJob: Job? = null
    private var pairReserveInfo: MarketPairReserveInfoDTO? = null

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
            cancelEstimateAmountJob()
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
        // 选择First Token
        if (selectFirst) {
            val currFirstToken = currFirstTokenLiveData.value
            if (selected == currFirstToken) return

            // 更新选择的First Token
            currFirstTokenLiveData.postValue(selected)

            val currSecondToken = currSecondTokenLiveData.value
            if (selected == currSecondToken) {
                // 交换Second Token位置
                currSecondTokenLiveData.postValue(currFirstToken)

                // 交换位置重新计算兑换率
                if (currFirstToken != null) {
                    calculateExchangeRate(selected.module)
                }
                return
            }

            // 转入模式下选择了新的交易对，获取交易对储备信息
            if (currSecondToken != null) {
                getPairReserveInfo(selected.module, currSecondToken.module)
            }
            return
        }

        // 选择Second Token
        val currSecondToken = currSecondTokenLiveData.value
        if (selected == currSecondToken) return

        // 更新选择的Second Token
        currSecondTokenLiveData.postValue(selected)

        val currFirstToken = currFirstTokenLiveData.value
        if (selected == currFirstToken) {
            // 交换First Token位置
            currFirstTokenLiveData.postValue(currSecondToken)

            // 交换位置重新计算兑换率
            if (currSecondToken != null) {
                calculateExchangeRate(currSecondToken.module)
            }
            return
        }

        // 转入模式下选择了新的交易对，获取交易对储备信息
        if (currFirstToken != null) {
            getPairReserveInfo(currFirstToken.module, selected.module)
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

            val liquidityToken = list[selectedPosition]
            currLiquidityTokenLiveData.postValue(liquidityToken)

            // 转出模式下选择了新的交易对，获取交易对储备信息
            getPairReserveInfo(liquidityToken.coinAName, liquidityToken.coinBName)
        }
    }

    //*********************************** 其它信息相关方法 ***********************************//
    private fun getPairReserveInfo(tokenAName: String, tokenBName: String) {
        pairReserveInfo = null
        exchangeRateLiveData.postValue(null)
        execute(
            tokenAName,
            tokenBName,
            action = ACTION_GET_PAIR_RESERVE_INFO
        )
    }

    private fun calculateExchangeRate(tokenAName: String) {
        pairReserveInfo?.let {
            val tokenAInFront = tokenAName == it.coinA.name
            exchangeRateLiveData.postValue(
                convertAmountToExchangeRate(
                    if (tokenAInFront) it.coinA.amount else it.coinB.amount,
                    if (tokenAInFront) it.coinB.amount else it.coinA.amount
                )
            )
        }
    }

    fun getExchangeRateLiveData(): LiveData<BigDecimal?> {
        return exchangeRateLiveData
    }

    fun getPoolTokenAndPoolShareLiveData(): LiveData<Pair<String, String>?> {
        return poolTokenAndPoolShareLiveData
    }

    fun setupViolasAccount(existsAccount: Boolean): Job {
        return viewModelScope.launch {
            if (!existsAccount) {
                violasAccountDO = null
                return@launch
            }

            val violasAccount = withContext(Dispatchers.IO) {
                AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
            } ?: return@launch

            violasAccountDO = violasAccount
        }
    }

    fun getViolasAccount(): AccountDO? {
        return violasAccountDO
    }

    fun getAccountManager(): AccountManager {
        return exchangeManager.mAccountManager
    }

    //*********************************** 输入金额联动方法 ***********************************//
    fun getFirstInputTextLiveData(): MutableLiveData<String> {
        return firstInputTextLiveData
    }

    fun getSecondInputTextLiveData(): MutableLiveData<String> {
        return secondInputTextLiveData
    }

    fun estimateFirstTokenTransferIntoAmount(secondInputAmount: String?) {
        pairReserveInfo ?: return
        currFirstTokenLiveData.value ?: return
        val secondToken = currSecondTokenLiveData.value ?: return

        cancelEstimateAmountJob()
        estimateAmountJob = viewModelScope.launch {
            delay(100)

            val result = withContext(Dispatchers.IO) {
                return@withContext if (secondInputAmount.isNullOrBlank())
                    ""
                else
                    calculateTransferIntoAmount(secondToken.module, secondInputAmount)
            }
            secondInputTextLiveData.postValue(result)

            estimateAmountJob = null
        }
    }

    fun estimateSecondTokenTransferIntoAmount(firstInputAmount: String?) {
        pairReserveInfo ?: return
        currSecondTokenLiveData.value ?: return
        val firstToken = currFirstTokenLiveData.value ?: return

        cancelEstimateAmountJob()
        estimateAmountJob = viewModelScope.launch {
            delay(100)

            val result = withContext(Dispatchers.IO) {
                return@withContext if (firstInputAmount.isNullOrBlank())
                    ""
                else
                    calculateTransferIntoAmount(firstToken.module, firstInputAmount)
            }
            secondInputTextLiveData.postValue(result)

            estimateAmountJob = null
        }
    }

    private fun calculateTransferIntoAmount(inputTokenName: String, inputAmount: String): String {
        return BigDecimal(inputAmount)
            .multiply(
                if (inputTokenName == pairReserveInfo!!.coinA.name)
                    pairReserveInfo!!.coinB.amount
                else
                    pairReserveInfo!!.coinA.amount
            )
            .divide(
                if (inputTokenName == pairReserveInfo!!.coinA.name)
                    pairReserveInfo!!.coinA.amount
                else
                    pairReserveInfo!!.coinB.amount,
                6,
                RoundingMode.DOWN
            )
            .stripTrailingZeros().toPlainString()
    }

    fun estimateTokensTransferOutAmount(inputLiquidityAmount: String?) {
        pairReserveInfo ?: return
        val liquidityToken = currLiquidityTokenLiveData.value ?: return

        cancelEstimateAmountJob()
        estimateAmountJob = viewModelScope.launch {
            delay(100)

            val result = withContext(Dispatchers.IO) {
                return@withContext if (inputLiquidityAmount.isNullOrBlank()) {
                    ""
                } else {
                    val amounts = calculateTransferOutAmounts(
                        liquidityToken.coinAName,
                        BigDecimal(inputLiquidityAmount)
                    )
                    "${amounts.first} ${liquidityToken.coinAName}\n${amounts.second} ${liquidityToken.coinBName}"
                }
            }
            secondInputTextLiveData.postValue(result)

            estimateAmountJob = null
        }
    }

    private fun calculateTransferOutAmounts(
        tokenAName: String,
        liquidityAmount: BigDecimal
    ): Pair<BigDecimal, BigDecimal> {
        val currencyAAmount = liquidityAmount
            .multiply(
                if (tokenAName == pairReserveInfo!!.coinA.name)
                    pairReserveInfo!!.coinA.amount
                else
                    pairReserveInfo!!.coinB.amount
            )
            .divide(pairReserveInfo!!.liquidityTotalAmount, 6, RoundingMode.DOWN)
            .stripTrailingZeros()
        val currencyBAmount = liquidityAmount
            .multiply(
                if (tokenAName == pairReserveInfo!!.coinA.name)
                    pairReserveInfo!!.coinB.amount
                else
                    pairReserveInfo!!.coinA.amount
            )
            .divide(pairReserveInfo!!.liquidityTotalAmount, 6, RoundingMode.DOWN)
            .stripTrailingZeros()
        return Pair(currencyAAmount, currencyBAmount)
    }

    private fun cancelEstimateAmountJob() {
        estimateAmountJob?.let {
            try {
                it.cancel()
            } catch (ignore: Exception) {
            }
            estimateAmountJob = null
        }
    }

    //*********************************** 耗时相关任务 ***********************************//
    override suspend fun realExecute(action: Int, vararg params: Any) {
        when (action) {
            ACTION_GET_USER_LIQUIDITY_TOKENS -> {
                val address = violasAccountDO!!.address
//                val address = "fa279f2615270daed6061313a48360f7"
                val userPoolInfo =
                    exchangeManager.mViolasService.getUserPoolInfo(address)
                liquidityTokensLiveData.postValue(userPoolInfo?.liquidityTokens)
            }

            ACTION_GET_PAIR_RESERVE_INFO -> {
                val pairReserveInfo =
                    exchangeManager.mViolasService.getMarketPairReserveInfo(
                        coinAName = params[0] as String,
                        coinBName = params[1] as String
                    )
                this.pairReserveInfo = pairReserveInfo

                val liquidityToken = currLiquidityTokenLiveData.value
                if (liquidityToken !== null) {
                    calculateExchangeRate(liquidityToken.coinAName)
                } else {
                    calculateExchangeRate(currFirstTokenLiveData.value!!.module)
                }
            }

            ACTION_ADD_LIQUIDITY -> {
                exchangeManager.addLiquidity(
                    privateKey = params[0] as ByteArray,
                    coinA = currFirstTokenLiveData.value!!,
                    coinB = currSecondTokenLiveData.value!!,
                    amountADesired = convertDisplayAmountToAmount(params[1] as String).toLong(),
                    amountBDesired = convertDisplayAmountToAmount(params[2] as String).toLong()
                )

                firstInputTextLiveData.postValue("")
                secondInputTextLiveData.postValue("")
            }

            ACTION_REMOVE_LIQUIDITY -> {
                val liquidityAmount = convertDisplayAmountToAmount(params[1] as String)
                val liquidityToken = currLiquidityTokenLiveData.value!!
                val amounts =
                    calculateTransferOutAmounts(liquidityToken.coinAName, liquidityAmount)
                // TODO 修改交易对中的币种信息
                /*exchangeManager.removeLiquidity(
                    privateKey = params[0] as ByteArray,
                    coinA = null,
                    coinB = null,
                    amountADesired = amounts.first.toLong(),
                    amountBDesired = amounts.second.toLong(),
                    liquidityAmount = liquidityAmount.toLong()
                )*/
            }

            else -> {
                error("Unsupported action: $action")
            }
        }
    }

    override fun isLoadAction(action: Int): Boolean {
        return action == ACTION_GET_USER_LIQUIDITY_TOKENS
                || action == ACTION_GET_PAIR_RESERVE_INFO
    }
}