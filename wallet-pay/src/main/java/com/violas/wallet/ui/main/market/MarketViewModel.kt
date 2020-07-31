package com.violas.wallet.ui.main.market

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.utils.toMap
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.ExchangeManager
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.PlatformTokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.wallet.utils.convertAmountToDisplayAmount
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsTokenVo
import com.violas.wallet.viewModel.bean.AssetsVo
import com.violas.wallet.viewModel.bean.HiddenTokenVo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * Created by elephant on 2020/7/8 13:00.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易市场的ViewModel
 */
class MarketViewModel : BaseViewModel() {

    private val exchangeManager by lazy {
        ExchangeManager()
    }
    private val appViewModel by lazy {
        WalletAppViewModel.getViewModelInstance()
    }

    private var marketCoinsLiveData = MediatorLiveData<List<ITokenVo>>()

    init {
        marketCoinsLiveData.addSource(appViewModel.mAssetsListLiveData) {
            val marketCoins = marketCoinsLiveData.value
            if (marketCoins.isNullOrEmpty()) return@addSource

            viewModelScope.launch(Dispatchers.IO) {
                setupBalance(it, marketCoins)
                marketCoinsLiveData.postValue(marketCoins)
            }
        }
    }

    fun getMarketSupportCoinsLiveData(): LiveData<List<ITokenVo>> {
        return marketCoinsLiveData
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        var marketCoins = marketCoinsLiveData.value
        if (!marketCoins.isNullOrEmpty()) return

        marketCoins = exchangeManager.getMarketSupportTokens()
        setupBalance(appViewModel.mAssetsListLiveData.value, marketCoins)
        marketCoinsLiveData.postValue(marketCoins)
    }

    private fun setupBalance(assetsList: List<AssetsVo>?, marketCoins: List<ITokenVo>) {
        if (assetsList.isNullOrEmpty()) return

        val assetsMap = assetsList.toMap { getAssetsKey(it) }
        marketCoins.forEach {
            it.displayAmount = convertAmountToDisplayAmount(
                assetsMap[getMarketCoinKey(it)]?.getAmount() ?: 0,
                CoinTypes.parseCoinType(it.coinNumber)
            )
        }
    }

    private fun getAssetsKey(assets: AssetsVo): String {
        return when (assets) {
            is AssetsTokenVo -> {
                "${assets.getCoinNumber()}${assets.name}"
            }
            is HiddenTokenVo -> {
                "${assets.getCoinNumber()}${assets.name}"
            }

            else -> {
                "${assets.getCoinNumber()}${assets.getAssetsName()}"
            }
        }
    }

    private fun getMarketCoinKey(coin: ITokenVo): String {
        return when (coin) {
            is StableTokenVo -> {
                "${coin.coinNumber}${coin.name}"
            }

            else -> {
                "${coin.coinNumber}${coin.displayName}"
            }
        }
    }

    private suspend fun mockLoadMarketCoins() {
        // test code
        delay(2000)
        val vls = PlatformTokenVo(
            coinNumber = CoinTypes.Violas.coinType(),
            displayName = "VLS",
            logo = "",
            displayAmount = BigDecimal(100_000000),
            anchorValue = 1.00,
            selected = false
        )

        val vlsusd = StableTokenVo(
            name = "VLSUSD",
            module = "VLSUSD",
            address = "00000000000000000000000000000000",
            marketIndex = 0,
            coinNumber = CoinTypes.Violas.coinType(),
            displayName = "VLSUSD",
            logo = "",
            displayAmount = BigDecimal(200_000000),
            anchorValue = 1.00,
            selected = false
        )

        val vlsgbp = StableTokenVo(
            name = "VLSGBP",
            module = "VLSGBP",
            address = "00000000000000000000000000000000",
            marketIndex = 0,
            coinNumber = CoinTypes.Violas.coinType(),
            displayName = "VLSGBP",
            logo = "",
            displayAmount = BigDecimal(300_000000),
            anchorValue = 1.2526,
            selected = false
        )

        val vlseur = StableTokenVo(
            name = "VLSEUR",
            module = "VLSEUR",
            address = "00000000000000000000000000000000",
            marketIndex = 0,
            coinNumber = CoinTypes.Violas.coinType(),
            displayName = "VLSEUR",
            logo = "",
            displayAmount = BigDecimal(400_000000),
            anchorValue = 1.1272,
            selected = false
        )

        val vlssgd = StableTokenVo(
            name = "VLSSGD",
            module = "VLSSGD",
            address = "00000000000000000000000000000000",
            marketIndex = 0,
            coinNumber = CoinTypes.Violas.coinType(),
            displayName = "VLSSGD",
            logo = "",
            displayAmount = BigDecimal(500_000000),
            anchorValue = 0.7167,
            selected = false
        )

        val vlsjpy = StableTokenVo(
            name = "VLSJPY",
            module = "VLSJPY",
            address = "00000000000000000000000000000000",
            marketIndex = 0,
            coinNumber = CoinTypes.Violas.coinType(),
            displayName = "VLSJPY",
            logo = "",
            displayAmount = BigDecimal(500_000000),
            anchorValue = 0.0093,
            selected = false
        )

        val lbr = PlatformTokenVo(
            coinNumber = CoinTypes.Libra.coinType(),
            displayName = "LBR",
            logo = "",
            displayAmount = BigDecimal(100_000000),
            anchorValue = 1.00,
            selected = false
        )

        val btc = PlatformTokenVo(
            coinNumber = CoinTypes.BitcoinTest.coinType(),
            displayName = "BTC",
            logo = "",
            displayAmount = BigDecimal(100_000000),
            anchorValue = 9000.00,
            selected = false
        )

        val list = mutableListOf(
            vls, vlsusd, vlsgbp, vlseur, vlssgd, vlsjpy, lbr, btc
        )
        marketCoinsLiveData.postValue(list)
    }
}