package com.violas.wallet.ui.mapping

import androidx.lifecycle.*
import com.palliums.base.BaseViewModel
import com.palliums.extensions.getShowErrorMessage
import com.palliums.extensions.isNoNetwork
import com.palliums.net.LoadState
import com.palliums.utils.toMap
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.mapping.MappingCoinPairDTO
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.PlatformTokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.wallet.utils.convertAmountToDisplayAmount
import com.violas.wallet.utils.str2CoinType
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsTokenVo
import com.violas.wallet.viewModel.bean.AssetsVo
import com.violas.wallet.viewModel.bean.HiddenTokenVo
import kotlinx.coroutines.*

/**
 * Created by elephant on 2020/8/12 19:01.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MappingViewModel : BaseViewModel() {

    // 当前选择的映射币种对
    private val currMappingCoinPairLiveData = MutableLiveData<MappingCoinPairDTO?>()

    // 可选择的映射币种对列表
    private val mappingCoinPairsLiveData = MutableLiveData<List<MappingCoinPairDTO>?>()
    private val coinsLiveData = MediatorLiveData<List<ITokenVo>>()

    private val mappingService by lazy { DataRepository.getMappingService() }
    private val appViewModel by lazy { WalletAppViewModel.getViewModelInstance() }

    val accountManager by lazy { AccountManager() }
    val coinsLoadState by lazy { EnhancedMutableLiveData<LoadState>() }
    val coinsTipsMessage by lazy { EnhancedMutableLiveData<String>() }

    init {
        coinsLiveData.addSource(appViewModel.mAssetsListLiveData) {
            val coins = coinsLiveData.value
            if (coins.isNullOrEmpty()) return@addSource

            viewModelScope.launch(Dispatchers.IO) {
                setupBalance(it, coins)
                coinsLiveData.postValue(coins)
            }
        }
    }

    fun getCurrMappingCoinPairLiveData(): LiveData<MappingCoinPairDTO?> {
        return currMappingCoinPairLiveData
    }

    fun selectCoin(selected: ITokenVo) {
        viewModelScope.launch {
            val selectedCoinPair = withContext(Dispatchers.IO) {
                coin2CoinPair(selected)
            } ?: return@launch

            val currCoinPair = currMappingCoinPairLiveData.value
            if (currCoinPair?.fromCoin?.chainName != selectedCoinPair.fromCoin.chainName
                || currCoinPair.fromCoin.assets.name != selectedCoinPair.fromCoin.assets.name
            ) {
                currMappingCoinPairLiveData.postValue(selectedCoinPair)
            }
        }
    }

    fun getMappingCoinPairsLiveData(): LiveData<List<MappingCoinPairDTO>?> {
        return mappingCoinPairsLiveData
    }

    fun getCoinsLiveData(): LiveData<List<ITokenVo>> {
        return coinsLiveData
    }

    suspend fun getAccount(): AccountDO? = coroutineScope {
        val coinPairs =
            currMappingCoinPairLiveData.value ?: return@coroutineScope null
        val coinNumber = str2CoinType(coinPairs.fromCoin.chainName).coinType()
        accountManager.getIdentityByCoinType(coinNumber)
    }

    fun coinPair2Coin(coinPair: MappingCoinPairDTO): ITokenVo {
        val coinType = str2CoinType(coinPair.fromCoin.chainName)
        return if (coinType == CoinTypes.Bitcoin || coinType == CoinTypes.BitcoinTest) {
            PlatformTokenVo(
                coinType.coinType(),
                coinPair.fromCoin.assets.displayName,
                coinPair.fromCoin.assets.logo
            )
        } else {
            StableTokenVo(
                coinPair.fromCoin.assets.name,
                coinPair.fromCoin.assets.module,
                coinPair.fromCoin.assets.address,
                -1,
                coinType.coinType(),
                coinPair.fromCoin.assets.displayName,
                coinPair.fromCoin.assets.logo
            )
        }
    }

    fun coin2CoinPair(coin: ITokenVo): MappingCoinPairDTO? {
        mappingCoinPairsLiveData.value?.forEach {
            if (coin.coinNumber == str2CoinType(it.fromCoin.chainName).coinType()) {
                if (coin is PlatformTokenVo
                    || (coin is StableTokenVo && coin.name == it.fromCoin.assets.name)
                ) {
                    return it
                }
            }
        }

        return null
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        // TODO 映射逻辑

    }

    fun getMappingCoinParis(failureCallback: ((error: Throwable) -> Unit)? = null): Boolean {
        if (coinsLoadState.value?.peekData()?.status == LoadState.Status.RUNNING) {
            return false
        }

        coinsLoadState.setValueSupport(LoadState.RUNNING)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    var coins = coinsLiveData.value
                    if (!coins.isNullOrEmpty()) return@withContext

                    var coinPairs = mappingCoinPairsLiveData.value
                    if (coinPairs.isNullOrEmpty()) {
                        coinPairs = mappingService.getMappingCoinPairs()
                    }

                    coins = mutableListOf()
                    coinPairs?.forEach {
                        coins.add(coinPair2Coin(it))
                    }
                    setupBalance(appViewModel.mAssetsListLiveData.value, coins)

                    mappingCoinPairsLiveData.postValue(coinPairs)
                    coinsLiveData.postValue(coins)
                }

                coinsLoadState.setValueSupport(LoadState.SUCCESS)
            } catch (e: Exception) {
                e.printStackTrace()

                if (e.isNoNetwork()) {
                    // 没有网络时返回很快，加载视图一闪而过效果不好
                    delay(500)
                }

                coinsLoadState.setValueSupport(LoadState.failure(e))
                coinsTipsMessage.setValueSupport(e.getShowErrorMessage(true))

                failureCallback?.invoke(e)
            }
        }
        return true
    }

    private fun setupBalance(assetsList: List<AssetsVo>?, coins: List<ITokenVo>) {
        if (assetsList.isNullOrEmpty()) return

        val assetsMap = assetsList.toMap { getAssetsKey(it) }
        coins.forEach {
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
}