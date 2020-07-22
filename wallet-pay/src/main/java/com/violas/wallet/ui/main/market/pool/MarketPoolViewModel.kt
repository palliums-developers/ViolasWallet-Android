package com.violas.wallet.ui.main.market.pool

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.violas.http.PoolLiquidityDTO
import com.palliums.violas.http.PoolLiquidityReserveInfoDTO
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
         * 获取用户可转出的流动资产列表
         */
        const val ACTION_GET_USER_LIQUIDITY_LIST = 0x01

        /**
         * 获取流动资产储备信息
         */
        const val ACTION_GET_LIQUIDITY_RESERVE_INFO = 0x02

        /**
         * 添加流动资产
         */
        const val ACTION_ADD_LIQUIDITY = 0x03

        /**
         * 移除流动资产
         */
        const val ACTION_REMOVE_LIQUIDITY = 0x04
    }

    // 当前的操作模式，分转入和转出
    private val currOpModeLiveData = MutableLiveData<MarketPoolOpMode>(MarketPoolOpMode.TransferIn)

    // 转入模式下选择的Coin
    private val currCoinALiveData = MediatorLiveData<StableTokenVo?>()
    private val currCoinBLiveData = MediatorLiveData<StableTokenVo?>()

    // 转出模式下选择的交易对和可转出的交易对列表
    private val currLiquidityLiveData = MediatorLiveData<PoolLiquidityDTO?>()
    private val liquidityListLiveData = MutableLiveData<List<PoolLiquidityDTO>?>()

    // 兑换率
    private val exchangeRateLiveData = MediatorLiveData<BigDecimal?>()

    // 资金池通证及占比
    private val poolTokenAndPoolShareLiveData = MediatorLiveData<Pair<String, String>?>()

    // 输入框文本
    private val inputTextALiveData = MutableLiveData<String>()
    private val inputTextBLiveData = MutableLiveData<String>()

    private val exchangeManager by lazy { ExchangeManager() }
    private var violasAccountDO: AccountDO? = null
    private var estimateAmountJob: Job? = null
    private var liquidityReserveInfo: PoolLiquidityReserveInfoDTO? = null

    init {
        currCoinALiveData.addSource(currOpModeLiveData) {
            currCoinALiveData.postValue(null)
            currCoinBLiveData.postValue(null)
            currLiquidityLiveData.postValue(null)

            exchangeRateLiveData.postValue(null)

            inputTextALiveData.postValue("")
            inputTextBLiveData.postValue("")
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
            inputTextBLiveData.postValue("")
        }
    }

    fun isTransferInMode(): Boolean {
        return currOpModeLiveData.value == MarketPoolOpMode.TransferIn
    }

    //*********************************** 转入模式下相关方法 ***********************************//
    fun getCurrCoinALiveData(): LiveData<StableTokenVo?> {
        return currCoinALiveData
    }

    fun getCurrCoinBLiveData(): LiveData<StableTokenVo?> {
        return currCoinBLiveData
    }

    fun selectCoin(selectCoinA: Boolean, selected: StableTokenVo) {
        // 选择 Coin A
        if (selectCoinA) {
            val currCoinA = currCoinALiveData.value
            if (selected == currCoinA) return

            // 更新选择的 Coin A
            currCoinALiveData.postValue(selected)

            val currCoinB = currCoinBLiveData.value
            if (selected == currCoinB) {
                // 交换 Coin B 位置
                currCoinBLiveData.postValue(currCoinA)

                // 交换位置重新计算兑换率
                if (currCoinA != null) {
                    calculateExchangeRate(selected.module)
                }
                return
            }

            // 转入模式下选择了新的交易对，获取交易对储备信息
            if (currCoinB != null) {
                getLiquidityReserveInfo(selected.module, currCoinB.module)
            }
            return
        }

        // 选择 Coin B
        val currCoinB = currCoinBLiveData.value
        if (selected == currCoinB) return

        // 更新选择的 Coin B
        currCoinBLiveData.postValue(selected)

        val currCoinA = currCoinALiveData.value
        if (selected == currCoinA) {
            // 交换 Coin A 位置
            currCoinALiveData.postValue(currCoinB)

            // 交换位置重新计算兑换率
            if (currCoinB != null) {
                calculateExchangeRate(currCoinB.module)
            }
            return
        }

        // 转入模式下选择了新的交易对，获取交易对储备信息
        if (currCoinA != null) {
            getLiquidityReserveInfo(currCoinA.module, selected.module)
        }
    }

    //*********************************** 转出模式下相关方法 ***********************************//
    fun getCurrLiquidityLiveData(): LiveData<PoolLiquidityDTO?> {
        return currLiquidityLiveData
    }

    fun getLiquidityListLiveData(): MutableLiveData<List<PoolLiquidityDTO>?> {
        return liquidityListLiveData
    }

    fun getCurrLiquidityPosition(): Int {
        val curr = currLiquidityLiveData.value ?: return -1
        val list = liquidityListLiveData.value ?: return -1
        list.forEachIndexed { index, item ->
            if (curr.coinA.marketIndex == item.coinA.marketIndex
                && curr.coinB.marketIndex == item.coinB.marketIndex
            ) {
                return index
            }
        }
        return -1
    }

    fun selectLiquidity(
        selectedPosition: Int,
        currPosition: Int = getCurrLiquidityPosition()
    ) {
        if (selectedPosition != currPosition) {
            val list = liquidityListLiveData.value ?: return
            if (selectedPosition < 0 || selectedPosition >= list.size) return

            val liquidity = list[selectedPosition]
            currLiquidityLiveData.postValue(liquidity)

            // 转出模式下选择了新的交易对，获取交易对储备信息
            getLiquidityReserveInfo(liquidity.coinA.module, liquidity.coinB.module)
        }
    }

    //*********************************** 其它信息相关方法 ***********************************//
    private fun getLiquidityReserveInfo(coinAModule: String, coinBModule: String) {
        liquidityReserveInfo = null
        exchangeRateLiveData.postValue(null)
        execute(
            coinAModule,
            coinBModule,
            action = ACTION_GET_LIQUIDITY_RESERVE_INFO
        )
    }

    private fun calculateExchangeRate(coinAModule: String) {
        liquidityReserveInfo?.let {
            val tokenAInFront = coinAModule == it.coinA.module
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
    fun getInputTextALiveData(): MutableLiveData<String> {
        return inputTextALiveData
    }

    fun getInputTextBLiveData(): MutableLiveData<String> {
        return inputTextBLiveData
    }

    fun estimateCoinATransferIntoAmount(secondInputAmountStr: String?) {
        liquidityReserveInfo ?: return
        currCoinALiveData.value ?: return
        val coinB = currCoinBLiveData.value ?: return

        cancelEstimateAmountJob()
        estimateAmountJob = viewModelScope.launch {
            delay(100)

            val result = withContext(Dispatchers.IO) {
                return@withContext if (secondInputAmountStr.isNullOrBlank())
                    ""
                else
                    calculateTransferIntoAmount(coinB.module, secondInputAmountStr)
            }
            inputTextBLiveData.postValue(result)

            estimateAmountJob = null
        }
    }

    fun estimateCoinBTransferIntoAmount(firstInputAmountStr: String?) {
        liquidityReserveInfo ?: return
        currCoinBLiveData.value ?: return
        val coinA = currCoinALiveData.value ?: return

        cancelEstimateAmountJob()
        estimateAmountJob = viewModelScope.launch {
            delay(100)

            val result = withContext(Dispatchers.IO) {
                return@withContext if (firstInputAmountStr.isNullOrBlank())
                    ""
                else
                    calculateTransferIntoAmount(coinA.module, firstInputAmountStr)
            }
            inputTextBLiveData.postValue(result)

            estimateAmountJob = null
        }
    }

    private fun calculateTransferIntoAmount(
        inputCoinModule: String,
        inputAmountStr: String
    ): String {
        return BigDecimal(inputAmountStr)
            .multiply(
                if (inputCoinModule == liquidityReserveInfo!!.coinA.module)
                    liquidityReserveInfo!!.coinB.amount
                else
                    liquidityReserveInfo!!.coinA.amount
            )
            .divide(
                if (inputCoinModule == liquidityReserveInfo!!.coinA.module)
                    liquidityReserveInfo!!.coinA.amount
                else
                    liquidityReserveInfo!!.coinB.amount,
                6,
                RoundingMode.DOWN
            )
            .stripTrailingZeros().toPlainString()
    }

    fun estimateCoinsTransferOutAmount(inputAmountStr: String?) {
        liquidityReserveInfo ?: return
        val liquidity = currLiquidityLiveData.value ?: return

        cancelEstimateAmountJob()
        estimateAmountJob = viewModelScope.launch {
            delay(100)

            val result = withContext(Dispatchers.IO) {
                return@withContext if (inputAmountStr.isNullOrBlank()) {
                    ""
                } else {
                    val amounts = calculateTransferOutAmounts(
                        liquidity.coinA.module,
                        BigDecimal(inputAmountStr)
                    )
                    "${amounts.first.toPlainString()} ${liquidity.coinA.displayName}\n${amounts.second.toPlainString()} ${liquidity.coinB.displayName}"
                }
            }
            inputTextBLiveData.postValue(result)

            estimateAmountJob = null
        }
    }

    private fun calculateTransferOutAmounts(
        coinAModule: String,
        liquidityAmount: BigDecimal
    ): Pair<BigDecimal, BigDecimal> {
        val currencyAAmount = liquidityAmount
            .multiply(
                if (coinAModule == liquidityReserveInfo!!.coinA.module)
                    liquidityReserveInfo!!.coinA.amount
                else
                    liquidityReserveInfo!!.coinB.amount
            )
            .divide(liquidityReserveInfo!!.liquidityTotalAmount, 6, RoundingMode.DOWN)
            .stripTrailingZeros()
        val currencyBAmount = liquidityAmount
            .multiply(
                if (coinAModule == liquidityReserveInfo!!.coinA.module)
                    liquidityReserveInfo!!.coinB.amount
                else
                    liquidityReserveInfo!!.coinA.amount
            )
            .divide(liquidityReserveInfo!!.liquidityTotalAmount, 6, RoundingMode.DOWN)
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
            ACTION_GET_USER_LIQUIDITY_LIST -> {
                val userPoolInfo =
                    exchangeManager.mViolasService.getUserPoolInfo(violasAccountDO!!.address)
                liquidityListLiveData.postValue(userPoolInfo?.liquidityList)
            }

            ACTION_GET_LIQUIDITY_RESERVE_INFO -> {
                val liquidityReserveInfo =
                    exchangeManager.mViolasService.getPoolLiquidityReserveInfo(
                        coinAModule = params[0] as String,
                        coinBModule = params[1] as String
                    )
                this.liquidityReserveInfo = liquidityReserveInfo

                if (isTransferInMode()) {
                    calculateExchangeRate(currCoinALiveData.value!!.module)
                } else {
                    calculateExchangeRate(currLiquidityLiveData.value!!.coinA.displayName)
                }
            }

            ACTION_ADD_LIQUIDITY -> {
                exchangeManager.addLiquidity(
                    privateKey = params[0] as ByteArray,
                    coinA = currCoinALiveData.value!!,
                    coinB = currCoinBLiveData.value!!,
                    amountADesired = convertDisplayAmountToAmount(params[1] as String).toLong(),
                    amountBDesired = convertDisplayAmountToAmount(params[2] as String).toLong()
                )

                liquidityReserveInfo = null
                inputTextALiveData.postValue("")
                inputTextBLiveData.postValue("")
                currCoinALiveData.postValue(null)
                currCoinBLiveData.postValue(null)
                exchangeRateLiveData.postValue(null)
            }

            ACTION_REMOVE_LIQUIDITY -> {
                val liquidityAmount = convertDisplayAmountToAmount(params[1] as String)
                val liquidity = currLiquidityLiveData.value!!
                val amounts =
                    calculateTransferOutAmounts(liquidity.coinA.displayName, liquidityAmount)
                exchangeManager.removeLiquidity(
                    privateKey = params[0] as ByteArray,
                    coinA = liquidity.coinA,
                    coinB = liquidity.coinB,
                    amountADesired = amounts.first.toLong(),
                    amountBDesired = amounts.second.toLong(),
                    liquidityAmount = liquidityAmount.toLong()
                )

                liquidityReserveInfo = null
                inputTextALiveData.postValue("")
                inputTextBLiveData.postValue("")
                currLiquidityLiveData.postValue(null)
                exchangeRateLiveData.postValue(null)
            }

            else -> {
                error("Unsupported action: $action")
            }
        }
    }

    override fun isLoadAction(action: Int): Boolean {
        return action == ACTION_GET_USER_LIQUIDITY_LIST
                || action == ACTION_GET_LIQUIDITY_RESERVE_INFO
    }
}