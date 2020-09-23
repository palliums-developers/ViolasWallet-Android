package com.violas.wallet.ui.main.market.swap

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.*
import com.palliums.base.BaseViewModel
import com.palliums.content.ContextProvider
import com.palliums.violas.bean.TokenMark
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.LibraTokenManager
import com.violas.wallet.biz.bean.AssertOriginateToken
import com.violas.wallet.biz.exchange.AssetsSwapManager
import com.violas.wallet.biz.exchange.SupportMappingSwapPairManager
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.ui.main.market.bean.*
import com.violas.wallet.utils.convertDisplayUnitToAmount
import kotlinx.coroutines.Dispatchers
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
class SwapViewModel() : BaseViewModel() {
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

    private val mAssetsSwapManager by lazy {
        AssetsSwapManager(
            SupportMappingSwapPairManager()
        )
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

    // <editor-fold defaultstate="collapsed" desc="兑换中心 Token 相关方法">
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
                currFromTokenLiveData.value = selected
                // 检查当前 FromToken 是否可以兑换 当前的 ToToken 不支持则清空
                viewModelScope.launch(Dispatchers.IO) {
                    val currToToken = currToTokenLiveData.value
                    if (selected == currToToken) {
                        withContext(Dispatchers.Main) {
                            currToTokenLiveData.value = null
                        }
                    } else {
                        currToToken?.let { toToken ->
                            var exists = false
                            getSwapToTokenList().forEach { token ->
                                if (IAssetsMark.convert(toToken)
                                        .equals(IAssetsMark.convert(token))
                                ) {
                                    exists = true
                                }
                            }
                            if (!exists) {
                                withContext(Dispatchers.Main) {
                                    currToTokenLiveData.value = null
                                }
                            }
                        }
                    }
                }
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

    /**
     * 获取兑换功能可以兑换转出的币种列表信息
     */
    fun getSwapToTokenList(): List<ITokenVo> {
        return if (currFromTokenLiveData.value != null) {
            mAssetsSwapManager.getSwapPayeeTokenList(currFromTokenLiveData.value!!)
        } else {
            arrayListOf()
        }
    }

    /**
     * 交易中心支持的币种信息发生变化调用该方法
     */
    fun onTokenChange(it: List<ITokenVo>?) {
        viewModelScope.launch {
            it?.let { it1 -> mAssetsSwapManager.calculateTokenMapInfo(it1) }
        }
    }

    /**
     * 检查 Token 映射信息，如果之前映射信息加载失败，则刷新映射信息。
     */
    fun checkTokenMapInfoAndRefresh() {
        if (mAssetsSwapManager.mSupportTokensLiveData.value?.isNotEmpty() == true &&
            mAssetsSwapManager.mMappingSupportSwapPairMapLiveData.value?.isEmpty() == true
        ) {
            viewModelScope.launch {
                mAssetsSwapManager.calculateTokenMapInfo(mAssetsSwapManager.mSupportTokensLiveData.value!!)
            }
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="其它信息相关方法">
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
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="兑换发起前的各种检查以及 publish 操作">
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
        val tokenManager = LibraTokenManager()

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
    // </editor-fold>

    //*********************************** 耗时相关任务 ***********************************//
    override suspend fun realExecute(action: Int, vararg params: Any) {
        // TODO 兑换逻辑
    }
}