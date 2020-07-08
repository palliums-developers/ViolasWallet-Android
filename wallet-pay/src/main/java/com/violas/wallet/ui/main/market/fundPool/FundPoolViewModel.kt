package com.violas.wallet.ui.main.market.fundPool

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.palliums.base.BaseViewModel
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import kotlinx.coroutines.delay
import java.math.BigDecimal

/**
 * Created by elephant on 2020/6/30 17:42.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class FundPoolViewModel : BaseViewModel() {

    companion object {
        const val ACTION_GET_TOKEN_PAIRS = 0x01
    }

    // 当前的操作模式，分转入和转出
    private val currOpModeLiveData = MutableLiveData<FundPoolOpMode>(FundPoolOpMode.TransferIn)

    // 转入模式下选择的通证
    private val currFirstTokenLiveData = MediatorLiveData<StableTokenVo?>()
    private val currSecondTokenLiveData = MediatorLiveData<StableTokenVo?>()

    // 转出模式下选择的交易对和可转出的交易对列表
    private val currTokenPairLiveData = MediatorLiveData<Pair<StableTokenVo, StableTokenVo>?>()
    private val tokenPairsLiveData = MutableLiveData<List<Pair<StableTokenVo, StableTokenVo>>?>()

    // 兑换率
    private val exchangeRateLiveData = MediatorLiveData<BigDecimal?>()

    // 资金池通证及占比
    private val poolTokenAndPoolShareLiveData = MediatorLiveData<Pair<String, String>?>()

    init {
        currFirstTokenLiveData.addSource(currOpModeLiveData) {
            if (it == FundPoolOpMode.TransferIn) {
                currFirstTokenLiveData.postValue(null)
            }
        }
        currSecondTokenLiveData.addSource(currOpModeLiveData) {
            if (it == FundPoolOpMode.TransferIn) {
                currSecondTokenLiveData.postValue(null)
            }
        }
        currTokenPairLiveData.addSource(currOpModeLiveData) {
            if (it == FundPoolOpMode.TransferOut) {
                currTokenPairLiveData.postValue(null)
            }
        }

        val convertToExchangeRate: (StableTokenVo, StableTokenVo) -> BigDecimal? =
            { firstToken, secondToken ->
                val firstUsdValue = BigDecimal(firstToken.anchorValue.toString())
                val secondUsdValue = BigDecimal(secondToken.anchorValue.toString())
                val zero = BigDecimal("0.00")
                if (firstUsdValue <= zero || secondUsdValue <= zero)
                    null
                else
                    firstUsdValue / secondUsdValue
            }
        exchangeRateLiveData.addSource(currFirstTokenLiveData) { firstToken ->
            val secondToken = currSecondTokenLiveData.value
            exchangeRateLiveData.postValue(
                if (firstToken == null || secondToken == null)
                    null
                else
                    convertToExchangeRate(firstToken, secondToken)
            )
        }
        exchangeRateLiveData.addSource(currSecondTokenLiveData) { secondToken ->
            val firstToken = currFirstTokenLiveData.value
            exchangeRateLiveData.postValue(
                if (firstToken == null || secondToken == null)
                    null
                else
                    convertToExchangeRate(firstToken, secondToken)
            )
        }
        exchangeRateLiveData.addSource(currTokenPairLiveData) {
            exchangeRateLiveData.postValue(
                if (it == null)
                    null
                else
                    convertToExchangeRate(it.first, it.second)
            )
        }
    }

    //*********************************** 操作模式相关方法 ***********************************//
    fun getCurrOpModeLiveData(): LiveData<FundPoolOpMode> {
        return currOpModeLiveData
    }

    fun getCurrOpModelPosition(): Int {
        return currOpModeLiveData.value?.ordinal ?: -1
    }

    fun switchOpModel(target: FundPoolOpMode) {
        if (target != currOpModeLiveData.value) {
            currOpModeLiveData.postValue(target)
        }
    }

    fun isTransferInMode(): Boolean {
        return currOpModeLiveData.value == FundPoolOpMode.TransferIn
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
    fun getCurrTokenPairLiveData(): LiveData<Pair<StableTokenVo, StableTokenVo>?> {
        return currTokenPairLiveData
    }

    fun getTokenPairsLiveData(): MutableLiveData<List<Pair<StableTokenVo, StableTokenVo>>?> {
        return tokenPairsLiveData
    }

    fun getCurrTokenPairPosition(): Int {
        val curr = currTokenPairLiveData.value ?: return -1
        val list = tokenPairsLiveData.value ?: return -1
        list.forEachIndexed { index, item ->
            if (curr.first == item.first && curr.second == item.second) {
                return index
            }
        }
        return -1
    }

    fun selectTokenPair(selectedPosition: Int, currPosition: Int = getCurrTokenPairPosition()) {
        if (selectedPosition != currPosition) {
            val list = tokenPairsLiveData.value ?: return
            if (selectedPosition < 0 || selectedPosition >= list.size) return
            currTokenPairLiveData.postValue(list[selectedPosition])
        }
    }

    //*********************************** 其它信息相关方法 ***********************************//
    fun getExchangeRateLiveData(): LiveData<BigDecimal?> {
        return exchangeRateLiveData
    }

    fun getPoolTokenAndPoolShareLiveData(): LiveData<Pair<String, String>?> {
        return poolTokenAndPoolShareLiveData
    }

    //*********************************** 耗时相关任务 ***********************************//
    override suspend fun realExecute(action: Int, vararg params: Any) {
        // 获取可转出的交易对列表
        if (action == ACTION_GET_TOKEN_PAIRS) {
            // test code
            delay(500)
            val vlsusd = StableTokenVo(
                accountDoId = 0,
                coinNumber = CoinTypes.Violas.coinType(),
                marketIndex = 0,
                tokenDoId = 0,
                address = "00000000000000000000000000000000",
                module = "VLSUSD",
                name = "VLSUSD",
                displayName = "VLSUSD",
                logoUrl = "",
                localEnable = true,
                chainEnable = true,
                amount = 200_000000,
                anchorValue = 1.00,
                selected = false
            )

            val vlsgbp = StableTokenVo(
                accountDoId = 0,
                coinNumber = CoinTypes.Violas.coinType(),
                marketIndex = 0,
                tokenDoId = 0,
                address = "00000000000000000000000000000000",
                module = "VLSGBP",
                name = "VLSGBP",
                displayName = "VLSGBP",
                logoUrl = "",
                localEnable = true,
                chainEnable = true,
                amount = 300_000000,
                anchorValue = 1.2504,
                selected = false
            )

            val vlseur = StableTokenVo(
                accountDoId = 0,
                coinNumber = CoinTypes.Violas.coinType(),
                marketIndex = 0,
                tokenDoId = 0,
                address = "00000000000000000000000000000000",
                module = "VLSEUR",
                name = "VLSEUR",
                displayName = "VLSEUR",
                logoUrl = "",
                localEnable = true,
                chainEnable = true,
                amount = 400_000000,
                anchorValue = 1.1319,
                selected = false
            )

            val vlssgd = StableTokenVo(
                accountDoId = 0,
                coinNumber = CoinTypes.Violas.coinType(),
                marketIndex = 0,
                tokenDoId = 0,
                address = "00000000000000000000000000000000",
                module = "VLSSGD",
                name = "VLSSGD",
                displayName = "VLSSGD",
                logoUrl = "",
                localEnable = true,
                chainEnable = true,
                amount = 500_000000,
                anchorValue = 0.7189,
                selected = false
            )
            val list = mutableListOf(
                Pair(vlsgbp, vlsusd),
                Pair(vlseur, vlsusd),
                Pair(vlssgd, vlsusd)
            )
            tokenPairsLiveData.postValue(list)
            return
        }

        // 转入
        if (isTransferInMode()) {
            val firstToken = currFirstTokenLiveData.value!!
            val secondToken = currSecondTokenLiveData.value!!
            return
        }

        // 转出
        val tokenPair = currTokenPairLiveData.value!!
    }
}