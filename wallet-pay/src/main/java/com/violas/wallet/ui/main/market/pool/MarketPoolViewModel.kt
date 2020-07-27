package com.violas.wallet.ui.main.market.pool

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.extensions.getShowErrorMessage
import com.palliums.extensions.isNoNetwork
import com.palliums.extensions.lazyLogError
import com.palliums.net.LoadState
import com.palliums.violas.http.PoolLiquidityDTO
import com.palliums.violas.http.PoolLiquidityReserveInfoDTO
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ExchangeManager
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.common.KEY_TWO
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.convertAmountToExchangeRate
import com.violas.wallet.utils.convertDisplayAmountToAmount
import kotlinx.coroutines.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by elephant on 2020/6/30 17:42.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MarketPoolViewModel : BaseViewModel(), Handler.Callback {

    companion object {
        /**
         * 获取用户可转出的流动资产列表
         */
        const val ACTION_GET_USER_LIQUIDITY_LIST = 0x01

        /**
         * 获取流动资产储备信息
         */
        const val ACTION_SYNC_LIQUIDITY_RESERVE_INFO = 0x02

        /**
         * 添加流动资产
         */
        const val ACTION_ADD_LIQUIDITY = 0x03

        /**
         * 移除流动资产
         */
        const val ACTION_REMOVE_LIQUIDITY = 0x04

        const val DELAY_SYNC_LIQUIDITY_RESERVE_INFO = 10 * 1000L
    }

    // 当前的操作模式，分转入和转出
    private val currOpModeLiveData = MutableLiveData<MarketPoolOpMode>(MarketPoolOpMode.TransferIn)

    // 转入模式下选择的Coin
    private val currCoinALiveData = MediatorLiveData<StableTokenVo?>()
    private val currCoinBLiveData = MutableLiveData<StableTokenVo?>()

    // 转出模式下选择的交易对和可转出的交易对列表
    private val currLiquidityLiveData = MutableLiveData<PoolLiquidityDTO?>()
    private val liquidityListLiveData = MutableLiveData<List<PoolLiquidityDTO>?>()

    // 流动资产的储备信息
    private val liquidityReserveInfoLiveData = MutableLiveData<PoolLiquidityReserveInfoDTO?>()
    private var syncLiquidityReserveInfoFlag = AtomicBoolean(false)

    // 兑换率
    private val exchangeRateLiveData = MediatorLiveData<BigDecimal?>()

    // 资金池通证及占比
    private val poolTokenAndPoolShareLiveData = MediatorLiveData<Pair<String, String>?>()

    // 输入框文本
    private val inputTextALiveData = MutableLiveData<String>()
    private val inputTextBLiveData = MutableLiveData<String>()

    private val exchangeManager by lazy { ExchangeManager() }
    private val handle by lazy { Handler(Looper.getMainLooper(), this) }
    private var violasAccountDO: AccountDO? = null
    private var estimateAmountJob: Job? = null

    init {
        currCoinALiveData.addSource(currOpModeLiveData) {
            currCoinALiveData.postValue(null)
            currCoinBLiveData.postValue(null)
            currLiquidityLiveData.postValue(null)
            liquidityReserveInfoLiveData.postValue(null)

            inputTextALiveData.postValue("")
            inputTextBLiveData.postValue("")
        }

        exchangeRateLiveData.addSource(liquidityReserveInfoLiveData) {
            if (it == null) {
                exchangeRateLiveData.postValue(null)
            } else {
                if (isTransferInMode()) {
                    calculateExchangeRate(currCoinALiveData.value!!.module, it)
                } else {
                    calculateExchangeRate(currLiquidityLiveData.value!!.coinA.module, it)
                }
            }
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
            //inputTextBLiveData.postValue("")
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

            // 转入模式下选择了新的交易对，开始同步流动资产的储备信息
            if (currCoinB != null) {
                startSyncLiquidityReserveInfoWork(selected.module, currCoinB.module)
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

        // 转入模式下选择了新的交易对，开始同步流动资产的储备信息
        if (currCoinA != null) {
            startSyncLiquidityReserveInfoWork(currCoinA.module, selected.module)
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

            // 转出模式下选择了新的交易对，开始同步流动资产的储备信息
            startSyncLiquidityReserveInfoWork(liquidity.coinA.module, liquidity.coinB.module)
        }
    }

    //*********************************** 其它信息相关方法 ***********************************//
    private fun calculateExchangeRate(
        coinAModule: String,
        liquidityReserveInfo: PoolLiquidityReserveInfoDTO? = liquidityReserveInfoLiveData.value
    ) {
        liquidityReserveInfo?.let {
            exchangeRateLiveData.postValue(
                convertAmountToExchangeRate(
                    if (coinAModule == it.coinA.module) it.coinA.amount else it.coinB.amount,
                    if (coinAModule == it.coinA.module) it.coinB.amount else it.coinA.amount
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

    fun calculateTransferIntoAmount(isInputA: Boolean, inputAmountStr: String?) {
        val coinA = currCoinALiveData.value ?: return
        val coinB = currCoinBLiveData.value ?: return
        val liquidityReserveInfo =
            liquidityReserveInfoLiveData.value ?: return

        cancelEstimateAmountJob()
        estimateAmountJob = viewModelScope.launch {
            delay(100)

            val result = withContext(Dispatchers.IO) {
                return@withContext if (inputAmountStr.isNullOrBlank())
                    ""
                else
                    estimateTransferIntoAmount(
                        if (isInputA) coinB.module else coinA.module,
                        inputAmountStr,
                        liquidityReserveInfo
                    )
            }

            if (isInputA)
                inputTextBLiveData.postValue(result)
            else
                inputTextALiveData.postValue(result)

            estimateAmountJob = null
        }
    }

    private fun estimateTransferIntoAmount(
        inputCoinModule: String,
        inputAmountStr: String,
        liquidityReserveInfo: PoolLiquidityReserveInfoDTO
    ): String {
        val amountA = convertDisplayAmountToAmount(inputAmountStr)
        val reserveA = if (inputCoinModule == liquidityReserveInfo.coinA.module)
            liquidityReserveInfo.coinA.amount
        else
            liquidityReserveInfo.coinB.amount
        val reserveB = if (inputCoinModule == liquidityReserveInfo.coinA.module)
            liquidityReserveInfo.coinB.amount
        else
            liquidityReserveInfo.coinA.amount
        val exchangeRate = convertAmountToExchangeRate(reserveA, reserveB)
        val amountB = amountA.multiply(exchangeRate)
            .setScale(0, RoundingMode.DOWN)
            .toPlainString()
        return convertAmountToDisplayAmountStr(amountB)
    }

    fun calculateTransferOutAmount(inputAmountStr: String?) {
        val liquidity = currLiquidityLiveData.value ?: return
        val liquidityReserveInfo =
            liquidityReserveInfoLiveData.value ?: return

        cancelEstimateAmountJob()
        estimateAmountJob = viewModelScope.launch {
            delay(100)

            val result = withContext(Dispatchers.IO) {
                return@withContext if (inputAmountStr.isNullOrBlank()) {
                    ""
                } else {
                    val amounts = estimateTransferOutAmounts(
                        liquidity.coinA.module,
                        BigDecimal(inputAmountStr),
                        liquidityReserveInfo
                    )
                    "${amounts.first.toPlainString()} ${liquidity.coinA.displayName}\n${amounts.second.toPlainString()} ${liquidity.coinB.displayName}"
                }
            }
            inputTextBLiveData.postValue(result)

            estimateAmountJob = null
        }
    }

    private fun estimateTransferOutAmounts(
        coinAModule: String,
        liquidityAmount: BigDecimal,
        liquidityReserveInfo: PoolLiquidityReserveInfoDTO
    ): Pair<BigDecimal, BigDecimal> {
        val coinAAmount = liquidityAmount
            .multiply(
                if (coinAModule == liquidityReserveInfo.coinA.module)
                    liquidityReserveInfo.coinA.amount
                else
                    liquidityReserveInfo.coinB.amount
            )
            .divide(liquidityReserveInfo.liquidityTotalAmount, 6, RoundingMode.DOWN)
            .stripTrailingZeros()
        val coinBAmount = liquidityAmount
            .multiply(
                if (coinAModule == liquidityReserveInfo.coinA.module)
                    liquidityReserveInfo.coinB.amount
                else
                    liquidityReserveInfo.coinA.amount
            )
            .divide(liquidityReserveInfo.liquidityTotalAmount, 6, RoundingMode.DOWN)
            .stripTrailingZeros()
        return Pair(coinAAmount, coinBAmount)
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

    //*********************************** 同步流动资金的储备信息 ***********************************//
    fun getLiquidityReserveInfoLiveData(): LiveData<PoolLiquidityReserveInfoDTO?> {
        return liquidityReserveInfoLiveData
    }

    fun startSyncLiquidityReserveInfoWork(
        coinAModuleSpecified: String? = null,
        coinBModuleSpecified: String? = null,
        showLoadingAndTips: Boolean = false
    ) {
        val coinAModule: String
        val coinBModule: String
        if (coinAModuleSpecified != null && coinBModuleSpecified != null) {
            coinAModule = coinAModuleSpecified
            coinBModule = coinBModuleSpecified
            liquidityReserveInfoLiveData.postValue(null)
        } else {
            if (isTransferInMode()) {
                coinAModule = currCoinALiveData.value?.module ?: return
                coinBModule = currCoinBLiveData.value?.module ?: return
            } else {
                coinAModule = currLiquidityLiveData.value?.coinA?.module ?: return
                coinBModule = currLiquidityLiveData.value?.coinB?.module ?: return
            }
        }

        lazyLogError {
            "startSyncLiquidityReserveInfoWork. coinAModule = $coinAModule, coinBModule = $coinBModule"
        }
        syncLiquidityReserveInfoFlag.set(true)
        handle.removeMessages(ACTION_SYNC_LIQUIDITY_RESERVE_INFO)
        handle.sendMessage(handle.obtainMessage(ACTION_SYNC_LIQUIDITY_RESERVE_INFO).apply {
            data = Bundle().apply {
                putString(KEY_ONE, coinAModule)
                putString(KEY_TWO, coinBModule)
            }
            arg1 = if (showLoadingAndTips) 1 else 0
        })
    }

    fun stopSyncLiquidityReserveInfoWork() {
        lazyLogError { "stopSyncLiquidityReserveInfoWork" }
        syncLiquidityReserveInfoFlag.set(false)
        handle.removeMessages(ACTION_SYNC_LIQUIDITY_RESERVE_INFO)
    }

    override fun handleMessage(msg: Message): Boolean {
        lazyLogError { "handleMessage. msg => $msg" }
        lazyLogError { "handleMessage. msg.data => ${msg.data}" }
        when (msg.what) {
            ACTION_SYNC_LIQUIDITY_RESERVE_INFO -> {
                if (!syncLiquidityReserveInfoFlag.get()) return true

                val coinAModule = msg.data.getString(KEY_ONE)
                val coinBModule = msg.data.getString(KEY_TWO)
                if (coinAModule.isNullOrBlank() || coinBModule.isNullOrBlank()) return true

                syncLiquidityReserveInfo(coinAModule, coinBModule, msg.arg1 == 1)

                handle.sendMessageDelayed(
                    handle.obtainMessage(ACTION_SYNC_LIQUIDITY_RESERVE_INFO).apply {
                        data = msg.data
                    },
                    DELAY_SYNC_LIQUIDITY_RESERVE_INFO
                )
            }
        }
        return true
    }

    private fun syncLiquidityReserveInfo(
        coinAModule: String,
        coinBModule: String,
        showLoadingAndTips: Boolean
    ) {
        viewModelScope.launch {
            if (showLoadingAndTips) {
                loadState.postValueSupport(LoadState.RUNNING.apply {
                    this.action = ACTION_SYNC_LIQUIDITY_RESERVE_INFO
                })
            }

            try {
                val liquidityReserveInfo = withContext(Dispatchers.IO) {
                    exchangeManager.mViolasService.getPoolLiquidityReserveInfo(
                        coinAModule, coinBModule
                    )
                }

                if (coinPairUnchanged(coinAModule, coinBModule)) {
                    liquidityReserveInfoLiveData.postValue(liquidityReserveInfo)
                }

                if (showLoadingAndTips) {
                    loadState.postValueSupport(LoadState.SUCCESS.apply {
                        this.action = ACTION_SYNC_LIQUIDITY_RESERVE_INFO
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()

                if (showLoadingAndTips) {
                    if (e.isNoNetwork()) {
                        // 没有网络时返回很快，加载视图一闪而过效果不好
                        delay(500)
                    }

                    loadState.postValueSupport(LoadState.failure(e).apply {
                        this.action = ACTION_SYNC_LIQUIDITY_RESERVE_INFO
                    })
                    tipsMessage.postValueSupport(e.getShowErrorMessage(true))
                }
            }
        }
    }

    private fun coinPairUnchanged(coinAModule: String, coinBModule: String): Boolean {
        if (isTransferInMode()) {
            val coinA = currCoinALiveData.value ?: return false
            val coinB = currCoinBLiveData.value ?: return false
            return coinA.module == coinAModule && coinB.module == coinBModule
                    || coinA.module == coinBModule && coinB.module == coinAModule
        }

        val liquidity = currLiquidityLiveData.value ?: return false
        return liquidity.coinA.module == coinAModule && liquidity.coinB.module == coinBModule
                || liquidity.coinA.module == coinBModule && liquidity.coinB.module == coinAModule
    }

    //*********************************** 耗时相关任务 ***********************************//
    override suspend fun realExecute(action: Int, vararg params: Any) {
        when (action) {
            ACTION_GET_USER_LIQUIDITY_LIST -> {
                val userPoolInfo =
                    exchangeManager.mViolasService.getUserPoolInfo(violasAccountDO!!.address)
                liquidityListLiveData.postValue(userPoolInfo?.liquidityList)
            }

            ACTION_ADD_LIQUIDITY -> {
                exchangeManager.addLiquidity(
                    privateKey = params[0] as ByteArray,
                    coinA = currCoinALiveData.value!!,
                    coinB = currCoinBLiveData.value!!,
                    amountADesired = convertDisplayAmountToAmount(params[1] as String).toLong(),
                    amountBDesired = convertDisplayAmountToAmount(params[2] as String).toLong()
                )

                inputTextALiveData.postValue("")
                inputTextBLiveData.postValue("")
                currCoinALiveData.postValue(null)
                currCoinBLiveData.postValue(null)
                liquidityReserveInfoLiveData.postValue(null)
            }

            ACTION_REMOVE_LIQUIDITY -> {
                val liquidityAmount = convertDisplayAmountToAmount(params[1] as String)
                val liquidity = currLiquidityLiveData.value!!
                val amounts = estimateTransferOutAmounts(
                    liquidity.coinA.module,
                    liquidityAmount,
                    liquidityReserveInfoLiveData.value!!
                )
                exchangeManager.removeLiquidity(
                    privateKey = params[0] as ByteArray,
                    coinA = liquidity.coinA,
                    coinB = liquidity.coinB,
                    amountADesired = amounts.first.toLong(),
                    amountBDesired = amounts.second.toLong(),
                    liquidityAmount = liquidityAmount.toLong()
                )

                inputTextALiveData.postValue("")
                inputTextBLiveData.postValue("")
                currLiquidityLiveData.postValue(null)
                liquidityReserveInfoLiveData.postValue(null)
            }

            else -> {
                error("Unsupported action: $action")
            }
        }
    }

    override fun isLoadAction(action: Int): Boolean {
        return action == ACTION_GET_USER_LIQUIDITY_LIST
    }
}