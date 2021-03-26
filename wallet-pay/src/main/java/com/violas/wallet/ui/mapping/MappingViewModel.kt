package com.violas.wallet.ui.mapping

import androidx.annotation.WorkerThread
import androidx.lifecycle.*
import com.palliums.base.BaseViewModel
import com.palliums.content.ContextProvider
import com.palliums.extensions.getShowErrorMessage
import com.palliums.extensions.isNoNetwork
import com.palliums.net.LoadState
import com.palliums.utils.toMap
import com.quincysx.crypto.CoinType
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.WrongPasswordException
import com.violas.wallet.biz.bean.AssertOriginateToken
import com.violas.wallet.biz.bean.DiemAppToken
import com.violas.wallet.biz.mapping.MappingManager
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.common.getBitcoinCoinType
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
import com.violas.wallet.viewModel.bean.AssetVo
import com.violas.wallet.viewModel.bean.CoinAssetVo
import com.violas.wallet.viewModel.bean.DiemCurrencyAssetVo
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
    private val appViewModel by lazy { WalletAppViewModel.getInstance() }
    private val mappingManager by lazy { MappingManager() }
    private val tokenManager by lazy { TokenManager() }

    val coinsLoadState by lazy { EnhancedMutableLiveData<LoadState>() }
    val coinsTipsMessage by lazy { EnhancedMutableLiveData<String>() }

    init {
        coinsLiveData.addSource(appViewModel.mAssetsLiveData) {
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
        return AccountManager.getAccountByCoinNumber(
            str2CoinType(
                if (fromCoin) coinPairs.fromCoin.chainName else coinPairs.toCoin.chainName
            ).coinNumber()
        )
    }

    fun coinPair2Coin(coinPair: MappingCoinPairDTO): ITokenVo {
        val coinType = str2CoinType(coinPair.fromCoin.chainName)
        return if (coinType == getBitcoinCoinType()) {
            PlatformTokenVo(
                coinType.coinNumber(),
                coinPair.fromCoin.assets.displayName,
                coinPair.fromCoin.assets.logo
            )
        } else {
            StableTokenVo(
                coinPair.fromCoin.assets.name,
                coinPair.fromCoin.assets.module,
                coinPair.fromCoin.assets.address,
                -1,
                coinType.coinNumber(),
                coinPair.fromCoin.assets.displayName,
                coinPair.fromCoin.assets.logo
            )
        }
    }

    fun coin2CoinPair(coin: ITokenVo): MappingCoinPairDTO? {
        mappingCoinPairsLiveData.value?.forEach {
            if (coin.coinNumber == str2CoinType(it.fromCoin.chainName).coinNumber()) {
                if (coin is PlatformTokenVo
                    || (coin is StableTokenVo && coin.module == it.fromCoin.assets.module)
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
                    setupBalance(appViewModel.mAssetsLiveData.value, coins)

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

    private fun setupBalance(assetsList: List<AssetVo>?, coins: List<ITokenVo>) {
        if (assetsList.isNullOrEmpty()) return

        val assetsMap = assetsList.toMap { getAssetsKey(it) }
        coins.forEach {
            it.displayAmount = convertAmountToDisplayAmount(
                assetsMap[getMarketCoinKey(it)]?.getAmount() ?: 0,
                CoinType.parseCoinNumber(it.coinNumber)
            )
        }
    }

    private fun getAssetsKey(assets: AssetVo): String {
        return when (assets) {
            is CoinAssetVo -> {
                "${assets.getCoinNumber()}${assets.getAssetsName()}"
            }
            is DiemCurrencyAssetVo -> {
                "${assets.getCoinNumber()}${assets.currency.module}"
            }
        }
    }

    private fun getMarketCoinKey(coin: ITokenVo): String {
        return when (coin) {
            is PlatformTokenVo -> {
                "${coin.coinNumber}${coin.displayName}"
            }
            is StableTokenVo -> {
                "${coin.coinNumber}${coin.module}"
            }
        }
    }
    // </editor-fold>

    // <editor-fold defaultState="collapsed" desc="映射及publish操作方法">
    suspend fun mapping(
        checkPayeeAccount: Boolean,
        payeeAddress: String?,
        payeeAccountDO: AccountDO?,
        payerAccountDO: AccountDO,
        password: ByteArray,
        amountStr: String
    ) {
        val coinPair = currMappingCoinPairLiveData.value!!
        mappingManager.mapping(
            checkPayeeAccount,
            payeeAddress,
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
        appToken: DiemAppToken
    ): Boolean {
        val privateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(password, accountDO.privateKey) ?: throw WrongPasswordException()

        val hasSucceed = tokenManager.publishToken(
            CoinType.parseCoinNumber(accountDO.coinNumber),
            privateKey,
            appToken.currency
        )
        if (hasSucceed) {
            tokenManager.insert(
                true, AssertOriginateToken(
                    appToken.currency,
                    accountId = accountDO.id,
                    name = appToken.name,
                    fullName = appToken.fullName,
                    isToken = true,
                    logo = appToken.logo
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