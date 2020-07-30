package com.violas.wallet.ui.main.market.swap

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.content.ContextProvider
import com.palliums.utils.coroutineExceptionHandler
import com.palliums.violas.bean.TokenMark
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.bean.AssertOriginateToken
import com.violas.wallet.biz.exchange.AssetsSwapManager
import com.violas.wallet.biz.exchange.NetWorkSupportTokensLoader
import com.violas.wallet.biz.exchange.SupportMappingSwapPairManager
import com.violas.wallet.common.SimpleSecurity
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
        const val MINIMUM_PRICE_FLUCTUATION = 1 / 100.0
    }

    // 输入输出选择的Token
    private val currFromTokenLiveData = MediatorLiveData<ITokenVo?>()
    private val currToTokenLiveData = MediatorLiveData<ITokenVo?>()

    // 手续费率
    private val handlingFeeRateLiveData = MediatorLiveData<BigDecimal?>()

    // 兑换率
    private val exchangeRateLiveData = MediatorLiveData<BigDecimal?>()

    // Gas费
    private val gasFeeLiveData = MediatorLiveData<String?>()

    private val mViolasService by lazy {
        DataRepository.getViolasService()
    }

    private val mAssetsSwapManager by lazy {
        AssetsSwapManager(NetWorkSupportTokensLoader(), SupportMappingSwapPairManager())
    }

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
                }
            }
        exchangeRateLiveData.addSource(currFromTokenLiveData) { fromToken ->
            handleCurrTokenChange(fromToken, currToTokenLiveData.value)
        }
        exchangeRateLiveData.addSource(currToTokenLiveData) { toToken ->
            handleCurrTokenChange(currFromTokenLiveData.value, toToken)
        }
    }

    suspend fun initSwapData(force: Boolean = false): Boolean {
        return mAssetsSwapManager.init(force)
    }

    //*********************************** Token相关方法 ***********************************//
    fun getCurrFromTokenLiveData(): LiveData<ITokenVo?> {
        return currFromTokenLiveData
    }

    fun getCurrToTokenLiveData(): LiveData<ITokenVo?> {
        return currToTokenLiveData
    }

    @MainThread
    fun selectToken(selectFrom: Boolean, selected: ITokenVo) {
        if (selectFrom) {
            val currFromToken = currFromTokenLiveData.value
            if (selected != currFromToken) {
                val currToToken = currToTokenLiveData.value
                if (selected == currToToken) {
                    currToTokenLiveData.value = null
                }
                currFromTokenLiveData.value = selected
            }
        } else {
            val currToToken = currToTokenLiveData.value
            if (selected != currToToken) {
                val currFromToken = currFromTokenLiveData.value
                if (selected == currFromToken) {
                    currFromTokenLiveData.value = currToToken
                }
                currToTokenLiveData.value = selected
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
    fun getHandlingFeeRateLiveDataLiveData(): MutableLiveData<BigDecimal?> {
        return handlingFeeRateLiveData
    }

    fun getExchangeRateLiveData(): MutableLiveData<BigDecimal?> {
        return exchangeRateLiveData
    }

    fun getGasFeeLiveData(): MutableLiveData<String?> {
        return gasFeeLiveData
    }

    fun getSupportTokensLiveData(): MutableLiveData<List<ITokenVo>?> {
        return mAssetsSwapManager.mSupportTokensLiveData
    }

    //*********************************** 其它信息相关方法 ***********************************//
    suspend fun swap(
        pwd: ByteArray,
        inputAmountStr: String,
        outputAmountStr: String,
        swapPath: List<Int>,
        isInputFrom: Boolean
    ) =
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

            val outputMiniAmount = if (isInputFrom) {
                outputAmount - (outputAmount * MINIMUM_PRICE_FLUCTUATION).toLong()
            } else {
                outputAmount
            }

            val swapPathByteArray = swapPath.map {
                it.and(0xFF).toByte()
            }.toByteArray()

            Log.e("==swap==", "amount:${outputMiniAmount}  path:${swapPath}")

            mAssetsSwapManager.swap(
                pwd,
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
        pwd: ByteArray,
        coinTypes: CoinTypes,
        assetsMark: ITokenVo
    ): Boolean {
        val identityByCoinType =
            AccountManager().getIdentityByCoinType(coinTypes.coinType()) ?: throw RuntimeException()

        assetsMark as StableTokenVo
        val tokenManager = TokenManager()

        val simpleSecurity =
            SimpleSecurity.instance(ContextProvider.getContext())

        val privateKey = simpleSecurity.decrypt(pwd, identityByCoinType.privateKey)
            ?: throw RuntimeException("password error")

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