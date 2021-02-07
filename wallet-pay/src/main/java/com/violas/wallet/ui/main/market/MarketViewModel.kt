package com.violas.wallet.ui.main.market

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.utils.toMap
import com.quincysx.crypto.CoinType
import com.violas.wallet.biz.ExchangeManager
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.wallet.utils.convertAmountToDisplayAmount
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsTokenVo
import com.violas.wallet.viewModel.bean.AssetsVo
import com.violas.wallet.viewModel.bean.HiddenTokenVo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                CoinType.parseCoinNumber(it.coinNumber)
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
}