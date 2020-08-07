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
import com.palliums.extensions.isActiveCancellation
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
import com.violas.wallet.utils.convertAmountToExchangeRate
import com.violas.wallet.utils.convertDisplayAmountToAmount
import kotlinx.coroutines.*
import java.math.BigDecimal
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
        const val ACTION_SYNC_LIQUIDITY_RESERVE = 0x02

        /**
         * 添加流动资产
         */
        const val ACTION_ADD_LIQUIDITY = 0x03

        /**
         * 移除流动资产
         */
        const val ACTION_REMOVE_LIQUIDITY = 0x04

        const val DELAY_SYNC_LIQUIDITY_RESERVE = 10 * 1000L

        const val TAG = "MarketPoolViewModel"
    }

    // 当前的操作模式，分转入和转出
    private val currOpModeLiveData = MutableLiveData<MarketPoolOpMode>(MarketPoolOpMode.TransferIn)

    // 转入模式下选择的Coin
    private val currCoinALiveData = MediatorLiveData<StableTokenVo?>()
    private val currCoinBLiveData = MutableLiveData<StableTokenVo?>()

    // 转出模式下选择的交易对和可转出的交易对列表
    private val currLiquidityLiveData = MutableLiveData<PoolLiquidityDTO?>()
    private val liquidityListLiveData = MutableLiveData<List<PoolLiquidityDTO>?>()

    // 流动资产储备信息
    private val liquidityReserveLiveData = MutableLiveData<PoolLiquidityReserveInfoDTO?>()
    private var syncLiquidityReserveFlag = AtomicBoolean(false)

    // 兑换率
    private val exchangeRateLiveData = MediatorLiveData<BigDecimal?>()

    // 资金池通证及占比
    private val poolTokenAndPoolShareLiveData = MediatorLiveData<Pair<String, String>?>()

    // 输入框文本
    private val inputATextLiveData = MutableLiveData<String>()
    private val inputBTextLiveData = MutableLiveData<String>()

    private val exchangeManager by lazy { ExchangeManager() }
    private val handler by lazy { Handler(Looper.getMainLooper(), this) }
    private var violasAccountDO: AccountDO? = null
    private var estimateAmountJob: Job? = null
    private var syncLiquidityReserveJob: Job? = null

    init {
        currCoinALiveData.addSource(currOpModeLiveData) {
            currCoinALiveData.value = null
            currCoinBLiveData.value = null
            currLiquidityLiveData.value = null
            liquidityReserveLiveData.value = null
            inputATextLiveData.value = ""
            inputBTextLiveData.value = ""

            cancelEstimateAmountJob()
            stopSyncLiquidityReserveWork()
        }

        exchangeRateLiveData.addSource(liquidityReserveLiveData) {
            calculateExchangeRate(
                if (isTransferInMode())
                    currCoinALiveData.value?.module
                else
                    currLiquidityLiveData.value?.coinA?.module,
                it
            )
        }
    }

    // <editor-fold defaultState="collapsed" desc="操作模式相关方法及逻辑">
    fun getCurrOpModeLiveData(): LiveData<MarketPoolOpMode> {
        return currOpModeLiveData
    }

    fun getCurrOpModelPosition(): Int {
        return currOpModeLiveData.value?.ordinal ?: -1
    }

    fun switchOpModel(target: MarketPoolOpMode) {
        viewModelScope.launch {
            if (target != currOpModeLiveData.value) {
                currOpModeLiveData.value = target
            }
        }
    }

    fun isTransferInMode(): Boolean {
        return currOpModeLiveData.value == MarketPoolOpMode.TransferIn
    }
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="转入模式下相关方法及逻辑">
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
            currCoinALiveData.value = selected

            val currCoinB = currCoinBLiveData.value
            if (selected == currCoinB) {
                // 交换 Coin B 位置
                currCoinBLiveData.value = currCoinA

                // 交换位置发送当前的流动资产储备信息，以重新估算金额和计算兑换率
                if (currCoinA != null) {
                    postCurrLiquidityReserve()
                }
                return
            }

            // 转入模式下选择了新的交易对，开始同步流动资产储备信息
            if (currCoinB != null) {
                startSyncLiquidityReserveWork(selected.module, currCoinB.module)
            }
            return
        }

        // 选择 Coin B
        val currCoinB = currCoinBLiveData.value
        if (selected == currCoinB) return

        // 更新选择的 Coin B
        currCoinBLiveData.value = selected

        val currCoinA = currCoinALiveData.value
        if (selected == currCoinA) {
            // 交换 Coin A 位置
            currCoinALiveData.value = currCoinB

            // 交换位置发送当前的流动资产储备信息，以重新估算金额和计算兑换率
            if (currCoinB != null) {
                postCurrLiquidityReserve()
            }
            return
        }

        // 转入模式下选择了新的交易对，开始同步流动资产储备信息
        if (currCoinA != null) {
            startSyncLiquidityReserveWork(currCoinA.module, selected.module)
        }
    }
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="转出模式下相关方法及逻辑">
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
        selectedPosition: Int
    ) {
        val list = liquidityListLiveData.value ?: return
        if (selectedPosition < 0 || selectedPosition >= list.size) return

        val selected = list[selectedPosition]
        val curr = currLiquidityLiveData.value
        if (selected == curr) return

        currLiquidityLiveData.value = selected

        // 转出模式下选择了新的交易对，开始同步流动资产储备信息
        if (curr == null
            || selected.coinA.module != curr.coinA.module
            || selected.coinB.module != curr.coinB.module
        ) {
            startSyncLiquidityReserveWork(selected.coinA.module, selected.coinB.module)
        }
    }
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="其它信息相关方法及逻辑">
    private fun calculateExchangeRate(
        coinAModule: String?,
        liquidityReserve: PoolLiquidityReserveInfoDTO? = liquidityReserveLiveData.value
    ) {
        lazyLogError(TAG) {
            "calculateExchangeRate. coin a module => $coinAModule" +
                    ", liquidity reserve => $liquidityReserve"
        }
        if (coinAModule.isNullOrBlank() || liquidityReserve == null) {
            exchangeRateLiveData.value = null
            return
        }

        liquidityReserve.let {
            exchangeRateLiveData.value = convertAmountToExchangeRate(
                if (coinAModule == it.coinA.module) it.coinA.amount else it.coinB.amount,
                if (coinAModule == it.coinA.module) it.coinB.amount else it.coinA.amount
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
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="输入金额联动相关方法及逻辑">
    fun getInputATextLiveData(): MutableLiveData<String> {
        return inputATextLiveData
    }

    fun getInputBTextLiveData(): MutableLiveData<String> {
        return inputBTextLiveData
    }

    fun estimateTransferIntoAmount(isInputA: Boolean, inputAmountStr: String?) {
        val coinA = currCoinALiveData.value ?: return
        val coinB = currCoinBLiveData.value ?: return
        val liquidityReserve = liquidityReserveLiveData.value ?: return

        cancelEstimateAmountJob()
        estimateAmountJob = viewModelScope.launch {
            delay(100)

            val result = withContext(Dispatchers.IO) {
                if (inputAmountStr.isNullOrBlank()
                    || BigDecimal(inputAmountStr) <= BigDecimal.ZERO
                ) {
                    ""
                } else {
                    exchangeManager.estimateAddLiquidityAmount(
                        if (isInputA) coinA.module else coinB.module,
                        inputAmountStr,
                        liquidityReserve
                    ).toPlainString()
                }
            }
            lazyLogError(TAG) {
                "estimateTransferIntoAmount. is input a => $isInputA" +
                        ", input amount => $inputAmountStr, output amount => $result"
            }

            if (isInputA)
                inputBTextLiveData.value = result
            else
                inputATextLiveData.value = result

            estimateAmountJob = null
        }
    }

    fun estimateTransferOutAmount(inputAmountStr: String?) {
        val liquidity = currLiquidityLiveData.value ?: return
        val liquidityReserve = liquidityReserveLiveData.value ?: return

        cancelEstimateAmountJob()
        estimateAmountJob = viewModelScope.launch {
            delay(100)

            val result = withContext(Dispatchers.IO) {
                if (inputAmountStr.isNullOrBlank()
                    || BigDecimal(inputAmountStr) <= BigDecimal.ZERO
                ) {
                    ""
                } else {
                    val amounts =
                        exchangeManager.estimateRemoveLiquidityAmounts(
                            liquidity.coinA.module,
                            BigDecimal(inputAmountStr),
                            liquidityReserve
                        )
                    "${amounts.first.toPlainString()} ${liquidity.coinA.displayName}" +
                            "\n${amounts.second.toPlainString()} ${liquidity.coinB.displayName}"
                }
            }
            lazyLogError(TAG) {
                "estimateTransferOutAmount. input amount => $inputAmountStr" +
                        ", output amount => $result"
            }

            inputBTextLiveData.value = result

            estimateAmountJob = null
        }
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
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="流动资产储备信息相关方法及逻辑">
    fun getLiquidityReserveLiveData(): LiveData<PoolLiquidityReserveInfoDTO?> {
        return liquidityReserveLiveData
    }

    fun startSyncLiquidityReserveWork(
        coinAModuleSpecified: String? = null,
        coinBModuleSpecified: String? = null,
        showLoadingAndTips: Boolean = false
    ) {
        stopSyncLiquidityReserveWork()

        val coinAModule: String
        val coinBModule: String
        if (coinAModuleSpecified != null && coinBModuleSpecified != null) {
            coinAModule = coinAModuleSpecified
            coinBModule = coinBModuleSpecified
            liquidityReserveLiveData.value = null
        } else {
            if (isTransferInMode()) {
                coinAModule = currCoinALiveData.value?.module ?: return
                coinBModule = currCoinBLiveData.value?.module ?: return
            } else {
                coinAModule = currLiquidityLiveData.value?.coinA?.module ?: return
                coinBModule = currLiquidityLiveData.value?.coinB?.module ?: return
            }
        }

        lazyLogError(TAG) {
            "startSyncLiquidityReserveWork. coin a module => $coinAModule" +
                    ", coin b module => $coinBModule"
        }
        syncLiquidityReserveFlag.set(true)
        handler.sendMessage(handler.obtainMessage(ACTION_SYNC_LIQUIDITY_RESERVE).apply {
            data = Bundle().apply {
                putString(KEY_ONE, coinAModule)
                putString(KEY_TWO, coinBModule)
            }
            arg1 = if (showLoadingAndTips) 1 else 0
        })
    }

    fun stopSyncLiquidityReserveWork() {
        if (!syncLiquidityReserveFlag.get()) return

        lazyLogError(TAG) { "stopSyncLiquidityReserveWork" }
        syncLiquidityReserveFlag.set(false)
        handler.removeMessages(ACTION_SYNC_LIQUIDITY_RESERVE)
        syncLiquidityReserveJob?.let {
            try {
                it.cancel()
            } catch (ignore: Exception) {
            }
            syncLiquidityReserveJob = null
        }
    }

    override fun handleMessage(msg: Message): Boolean {
        lazyLogError(TAG) { "handleMessage. msg => $msg, msg.data => ${msg.data}" }
        when (msg.what) {
            ACTION_SYNC_LIQUIDITY_RESERVE -> {
                if (!syncLiquidityReserveFlag.get()) return true

                val coinAModule = msg.data.getString(KEY_ONE)
                val coinBModule = msg.data.getString(KEY_TWO)
                if (coinAModule.isNullOrBlank() || coinBModule.isNullOrBlank()) return true

                syncLiquidityReserve(coinAModule, coinBModule, msg.arg1 == 1)

                handler.sendMessageDelayed(
                    handler.obtainMessage(ACTION_SYNC_LIQUIDITY_RESERVE).apply {
                        data = msg.data
                    },
                    DELAY_SYNC_LIQUIDITY_RESERVE
                )
            }
        }
        return true
    }

    private fun syncLiquidityReserve(
        coinAModule: String,
        coinBModule: String,
        showLoadingAndTips: Boolean
    ) {
        syncLiquidityReserveJob = viewModelScope.launch {
            if (showLoadingAndTips) {
                loadState.setValueSupport(LoadState.RUNNING.apply {
                    this.action = ACTION_SYNC_LIQUIDITY_RESERVE
                })
            }

            try {
                val liquidityReserve =
                    exchangeManager.mViolasService.getPoolLiquidityReserve(
                        coinAModule, coinBModule
                    )
                lazyLogError(TAG) { "syncLiquidityReserve. liquidity reserve => $liquidityReserve" }

                val syncWorkUnstopped = syncLiquidityReserveFlag.get()
                lazyLogError(TAG) { "syncLiquidityReserve. sync work unstopped => $syncWorkUnstopped" }

                val coinPairUnchanged = coinPairUnchanged(coinAModule, coinBModule)
                lazyLogError(TAG) { "syncLiquidityReserve. coin pair unchanged => $coinPairUnchanged" }

                if (syncWorkUnstopped && coinPairUnchanged) {
                    liquidityReserveLiveData.value = liquidityReserve
                }

                if (showLoadingAndTips) {
                    loadState.setValueSupport(LoadState.SUCCESS.apply {
                        this.action = ACTION_SYNC_LIQUIDITY_RESERVE
                    })
                }
            } catch (e: Exception) {
                lazyLogError(e, TAG) { "syncLiquidityReserve. sync failed" }

                if (showLoadingAndTips) {
                    if (e.isActiveCancellation()) {
                        loadState.setValueSupport(LoadState.SUCCESS.apply {
                            this.action = ACTION_SYNC_LIQUIDITY_RESERVE
                        })
                    } else {
                        if (e.isNoNetwork()) {
                            // 没有网络时返回很快，加载视图一闪而过效果不好
                            delay(500)
                        }

                        loadState.setValueSupport(LoadState.failure(e).apply {
                            this.action = ACTION_SYNC_LIQUIDITY_RESERVE
                        })
                        tipsMessage.setValueSupport(e.getShowErrorMessage(true))
                    }
                }
            }

            syncLiquidityReserveJob = null
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

    private fun postCurrLiquidityReserve() {
        val curr = liquidityReserveLiveData.value
        liquidityReserveLiveData.value = curr
    }
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="耗时操作相关逻辑">
    override suspend fun realExecute(action: Int, vararg params: Any) {
        when (action) {
            ACTION_GET_USER_LIQUIDITY_LIST -> {
                val userPoolInfo =
                    exchangeManager.mViolasService.getUserPoolInfo(violasAccountDO!!.address)
                withContext(Dispatchers.Main) {
                    liquidityListLiveData.value = userPoolInfo?.liquidityList
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

                withContext(Dispatchers.Main) {
                    inputATextLiveData.value = ""
                    inputBTextLiveData.value = ""
                }
            }

            ACTION_REMOVE_LIQUIDITY -> {
                val liquidityAmount = convertDisplayAmountToAmount(params[1] as String)
                val liquidity = currLiquidityLiveData.value!!
                val amounts =
                    exchangeManager.estimateRemoveLiquidityAmounts(
                        liquidity.coinA.module,
                        liquidityAmount,
                        liquidityReserveLiveData.value!!
                    )
                exchangeManager.removeLiquidity(
                    privateKey = params[0] as ByteArray,
                    coinA = liquidity.coinA,
                    coinB = liquidity.coinB,
                    amountADesired = amounts.first.toLong(),
                    amountBDesired = amounts.second.toLong(),
                    liquidityAmount = liquidityAmount.toLong()
                )

                withContext(Dispatchers.Main) {
                    inputATextLiveData.value = ""
                    inputBTextLiveData.value = ""
                    currLiquidityLiveData.value = null
                    liquidityReserveLiveData.value = null
                }
            }

            else -> {
                error("Unsupported action: $action")
            }
        }
    }
    // </editor-fold>

    override fun isLoadAction(action: Int): Boolean {
        return action == ACTION_GET_USER_LIQUIDITY_LIST
    }
}