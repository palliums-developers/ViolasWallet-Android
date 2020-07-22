package com.violas.wallet.ui.main.market.swap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.utils.coroutineExceptionHandler
import com.palliums.violas.bean.TokenMark
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.bean.AssertOriginateToken
import com.violas.wallet.biz.exchange.AssetsSwapManager
import com.violas.wallet.biz.exchange.NetWorkSupportTokensLoader
import com.violas.wallet.biz.exchange.SupportMappingSwapPairManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.main.market.bean.*
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.utils.convertDisplayUnitToAmount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.RuntimeException
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Created by elephant on 2020/6/30 17:42.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class SwapViewModel : BaseViewModel() {
    companion object {
        // 最低价格浮动汇率
        private const val MINIMUM_PRICE_FLUCTUATION = 1 / 100
    }

    // 输入输出选择的Token
    private val currFromTokenLiveData = MediatorLiveData<ITokenVo?>()
    private val currToTokenLiveData = MediatorLiveData<ITokenVo?>()

    //兑换路径
    private val swapPath = arrayListOf<Int>()

    // 手续费率
    private val handlingFeeRateLiveData = MediatorLiveData<BigDecimal?>()

    // 兑换率
    private val exchangeRateLiveData = MediatorLiveData<BigDecimal?>()

    // Gas费
    private val gasFeeLiveData = MediatorLiveData<Long?>()

    private val mViolasService by lazy {
        DataRepository.getViolasService()
    }

    private val mAssetsSwapManager by lazy {
        AssetsSwapManager(NetWorkSupportTokensLoader(), SupportMappingSwapPairManager())
    }

    private var exchangeSwapTrialJob: Job? = null

    init {
        val convertToExchangeRate: (ITokenVo, ITokenVo) -> BigDecimal? =
            { fromToken, toToken ->
                val fromAnchorValue = BigDecimal(fromToken.anchorValue.toString())
                val toAnchorValue = BigDecimal(toToken.anchorValue.toString())
                val zero = BigDecimal("0.00")
                if (fromAnchorValue <= zero || toAnchorValue <= zero)
                    null
                else
                    fromAnchorValue.divide(toAnchorValue, 8, RoundingMode.DOWN)
                        .stripTrailingZeros()
            }
        val handleCurrTokenChange: (ITokenVo?, ITokenVo?) -> Unit =
            { fromToken, toToken ->
                if (fromToken == null || toToken == null) {
                    handlingFeeRateLiveData.postValue(null)
                    exchangeRateLiveData.postValue(null)
                    gasFeeLiveData.postValue(null)
                } else {
                    handlingFeeRateLiveData.postValue(
                        if (fromToken.coinNumber != CoinTypes.Violas.coinType()
                            || toToken.coinNumber != CoinTypes.Violas.coinType()
                        )
                            BigDecimal("0.3")
                        else
                            BigDecimal("0.00591")
                    )
                    exchangeRateLiveData.postValue(convertToExchangeRate(fromToken, toToken))
                    gasFeeLiveData.postValue(0)
                }
            }
        exchangeRateLiveData.addSource(currFromTokenLiveData) { fromToken ->
            handleCurrTokenChange(fromToken, currToTokenLiveData.value)
        }
        exchangeRateLiveData.addSource(currToTokenLiveData) { toToken ->
            handleCurrTokenChange(currFromTokenLiveData.value, toToken)
        }
    }

    suspend fun initSwapData(): Boolean {
        return withContext(Dispatchers.IO) {
            mAssetsSwapManager.init()
        }
    }

    //*********************************** Token相关方法 ***********************************//
    fun getCurrFromTokenLiveData(): LiveData<ITokenVo?> {
        return currFromTokenLiveData
    }

    fun getCurrToTokenLiveData(): LiveData<ITokenVo?> {
        return currToTokenLiveData
    }

    fun selectToken(selectFrom: Boolean, selected: ITokenVo) {
        if (selectFrom) {
            val currFromToken = currFromTokenLiveData.value
            if (selected != currFromToken) {
                val currToToken = currToTokenLiveData.value
                if (selected == currToToken) {
                    currToTokenLiveData.postValue(currFromToken)
                }
                currFromTokenLiveData.postValue(selected)
            }
        } else {
            val currToToken = currToTokenLiveData.value
            if (selected != currToToken) {
                val currFromToken = currFromTokenLiveData.value
                if (selected == currFromToken) {
                    currFromTokenLiveData.postValue(currToToken)
                }
                currToTokenLiveData.postValue(selected)
            }
        }
    }

    fun getSwapToTokenList(): List<ITokenVo> {
        return if (currFromTokenLiveData.value != null) {
            mAssetsSwapManager.getSwapPayeeTokenList(currFromTokenLiveData.value!!)
        } else {
            arrayListOf()
        }
    }

    //*********************************** 其它信息相关方法 ***********************************//
    fun getHandlingFeeRateLiveDataLiveData(): LiveData<BigDecimal?> {
        return handlingFeeRateLiveData
    }

    fun getExchangeRateLiveData(): LiveData<BigDecimal?> {
        return exchangeRateLiveData
    }

    fun getGasFeeLiveData(): LiveData<Long?> {
        return gasFeeLiveData
    }

    fun getSupportTokensLiveData(): MutableLiveData<List<ITokenVo>?> {
        return mAssetsSwapManager.mSupportTokensLiveData
    }

    //*********************************** 其它信息相关方法 ***********************************//
    /**
     * 兑换手续费用预估
     */
    fun exchangeSwapTrial(inputAmountStr: String, callback: (String, String) -> Unit) {
        val inputToken = currFromTokenLiveData.value
        val outputToken = currToTokenLiveData.value

        swapPath.clear()

        if (inputToken == null || outputToken == null) {
            return
        }

        val inputAmount = convertDisplayUnitToAmount(
            inputAmountStr,
            CoinTypes.parseCoinType(inputToken.coinNumber)
        )

        if (inputAmount == 0L) {
            return
        }

        exchangeSwapTrialJob?.cancel()
        exchangeSwapTrialJob = viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler()) {

            mViolasService.exchangeSwapTrial(
                inputAmount,
                getCoinName(inputToken),
                getCoinName(outputToken)
            )?.let { exchangeSwapTrial ->
                swapPath.addAll(exchangeSwapTrial.path)
                val outputAmount = convertAmountToDisplayUnit(
                    exchangeSwapTrial.amount,
                    CoinTypes.parseCoinType(outputToken.coinNumber)
                )
                val outputFee = convertAmountToDisplayUnit(
                    exchangeSwapTrial.fee,
                    CoinTypes.parseCoinType(outputToken.coinNumber)
                )
                withContext(Dispatchers.Main) {
                    callback(outputAmount.first, outputFee.first)
                }
            }
        }
    }

    private fun getCoinName(iTokenVo: ITokenVo): String {
        return when (iTokenVo) {
            is PlatformTokenVo -> CoinTypes.parseCoinType(iTokenVo.coinNumber).coinName()
            is StableTokenVo -> iTokenVo.module
            else -> ""
        }
    }

    suspend fun swap(key: ByteArray, inputAmountStr: String, outputAmountStr: String) =
        withContext(Dispatchers.IO) {
            val inputToken = currFromTokenLiveData.value!!
            val outputToken = currToTokenLiveData.value!!

            val inputAmount =
                convertDisplayUnitToAmount(
                    inputAmountStr,
                    CoinTypes.parseCoinType(inputToken.coinNumber)
                )
            val outputAmount =
                convertDisplayUnitToAmount(
                    outputAmountStr,
                    CoinTypes.parseCoinType(outputToken.coinNumber)
                )

            val outputMiniAmount = outputAmount - outputAmount * MINIMUM_PRICE_FLUCTUATION

            val swapPathByteArray = swapPath.map {
                it.and(0xFF).toByte()
            }.toByteArray()

            mAssetsSwapManager.swap(
                key,
                inputToken,
                outputToken,
                inputAmount,
                outputMiniAmount,
                swapPathByteArray,
                byteArrayOf()
            )
        }

    //*********************************** 耗时相关任务 ***********************************//
    override suspend fun realExecute(action: Int, vararg params: Any) {
        // TODO 兑换逻辑
    }

    /**
     * publish Token 操作
     */
    @Throws(RuntimeException::class)
    suspend fun publishToken(
        privateKey: ByteArray,
        coinTypes: CoinTypes,
        assetsMark: ITokenVo
    ): Boolean {
        val identityByCoinType =
            AccountManager().getIdentityByCoinType(coinTypes.coinType()) ?: throw RuntimeException()

        assetsMark as StableTokenVo
        val tokenManager = TokenManager()

        val tokenMark = TokenMark(assetsMark.module, assetsMark.address, assetsMark.name)
        val hasSucceed = tokenManager.publishToken(
            coinTypes,
            privateKey,
            tokenMark
        )
        if (hasSucceed) {
            tokenManager.insert(
                true, AssertOriginateToken(
                    tokenMark,
                    account_id = identityByCoinType.id,
                    name = assetsMark.displayName,
                    fullName = assetsMark.displayName,
                    isToken = true,
                    logo = assetsMark.logo
                )
            )
        }
        return hasSucceed
    }
}