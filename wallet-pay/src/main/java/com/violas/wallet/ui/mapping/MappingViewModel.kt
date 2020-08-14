package com.violas.wallet.ui.mapping

import androidx.annotation.WorkerThread
import androidx.lifecycle.*
import com.palliums.base.BaseViewModel
import com.palliums.content.ContextProvider
import com.palliums.extensions.getShowErrorMessage
import com.palliums.extensions.isNoNetwork
import com.palliums.net.LoadState
import com.palliums.utils.toMap
import com.palliums.violas.bean.TokenMark
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.bean.AssertOriginateToken
import com.violas.wallet.biz.mapping.MappingManager
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.mapping.MappingCoinPairDTO
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.PlatformTokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.wallet.utils.convertAmountToDisplayAmount
import com.violas.wallet.utils.convertDisplayAmountToAmount
import com.violas.wallet.utils.str2CoinType
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsTokenVo
import com.violas.wallet.viewModel.bean.AssetsVo
import com.violas.wallet.viewModel.bean.HiddenTokenVo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val mappingManager by lazy { MappingManager() }
    private val tokenManager by lazy { TokenManager() }

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

    // <editor-fold defaultState="collapsed" desc="选择币种方法及逻辑">
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
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="辅助方法">
    @WorkerThread
    fun getAccount(fromCoin: Boolean): AccountDO? {
        val coinPairs =
            currMappingCoinPairLiveData.value ?: return null
        return accountManager.getIdentityByCoinType(
            str2CoinType(
                if (fromCoin) coinPairs.fromCoin.chainName else coinPairs.toCoin.chainName
            ).coinType()
        )
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
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="获取映射币种对信息方法及逻辑">
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
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="映射及publish操作方法">
    suspend fun mapping(
        checkPayeeAccount: Boolean,
        payeeAccountDO: AccountDO,
        payerAccountDO: AccountDO,
        password: ByteArray,
        amountStr: String
    ) {
        val coinPair = currMappingCoinPairLiveData.value!!
        mappingManager.mapping(
            checkPayeeAccount,
            payeeAccountDO,
            payerAccountDO,
            password,
            convertDisplayAmountToAmount(
                amountStr,
                str2CoinType(coinPair.fromCoin.chainName)
            ).toLong(),
            coinPair
        )
    }

    suspend fun publishToken(
        password: ByteArray,
        accountDO: AccountDO,
        assets: MappingCoinPairDTO.Assets
    ): Boolean {
        val simpleSecurity =
            SimpleSecurity.instance(ContextProvider.getContext())
        val privateKey = simpleSecurity.decrypt(password, accountDO.privateKey)!!

        val tokenMark = TokenMark(assets.module, assets.address, assets.name)
        val hasSucceed = tokenManager.publishToken(
            CoinTypes.parseCoinType(accountDO.coinNumber),
            privateKey,
            tokenMark
        )
        if (hasSucceed) {
            tokenManager.insert(
                true, AssertOriginateToken(
                    tokenMark,
                    account_id = accountDO.id,
                    name = assets.displayName,
                    fullName = assets.displayName,
                    isToken = true,
                    logo = assets.logo
                )
            )
        }
        return hasSucceed
    }
    // </editor-fold>

    override suspend fun realExecute(action: Int, vararg params: Any) {
        // ignore
    }
}