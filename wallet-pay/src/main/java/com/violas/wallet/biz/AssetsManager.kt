package com.violas.wallet.biz

import android.content.Context
import androidx.annotation.WorkerThread
import com.palliums.content.ContextProvider.getContext
import com.palliums.utils.exceptionAsync
import com.palliums.utils.toMap
import com.quincysx.crypto.CoinType
import com.violas.wallet.biz.bean.DiemCurrency
import com.violas.wallet.common.SP_FILE_NAME_FIAT_RATES
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountType
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.viewModel.bean.AssetVo
import com.violas.wallet.viewModel.bean.CoinAssetVo
import com.violas.wallet.viewModel.bean.DiemCoinAssetVo
import com.violas.wallet.viewModel.bean.DiemCurrencyAssetVo
import kotlinx.coroutines.GlobalScope
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

/**
 * Created by elephant on 3/18/21 3:04 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class AssetsManager {

    private val fiatRatesSharedPreferences by lazy {
        getContext().getSharedPreferences(SP_FILE_NAME_FIAT_RATES, Context.MODE_PRIVATE)
    }

    /**
     * 获取本地资产
     */
    @WorkerThread
    fun getLocalAssets(): MutableList<AssetVo> {
        val localAssets = mutableListOf<AssetVo>()
        val accounts = AccountManager.getAccountStorage().loadAll()
        accounts.forEach { account ->
            when (account.coinNumber) {
                getViolasCoinType().coinNumber(), getDiemCoinType().coinNumber() -> {
                    localAssets.add(
                        DiemCoinAssetVo(
                            account.id,
                            account.coinNumber,
                            account.address,
                            account.amount,
                            account.logo,
                            authKeyPrefix = account.authKeyPrefix
                        ).apply {
                            setAssetsName(CoinType.parseCoinNumber(account.coinNumber).coinName())
                        }
                    )
                }

                getBitcoinCoinType().coinNumber() -> {
                    localAssets.add(
                        CoinAssetVo(
                            account.id,
                            account.coinNumber,
                            account.address,
                            account.amount,
                            account.logo,
                            account.accountType
                        ).apply {
                            setAssetsName(CoinType.parseCoinNumber(account.coinNumber).coinName())
                            calculateDisplayAmount(this)
                            calculateFiatAmount(this)
                        }
                    )
                }
            }
        }

        val accountMap = accounts.toMap { it.id.toString() }
        val localTokens = DataRepository.getTokenStorage().loadEnableAll()
        localTokens.forEach { token ->
            val account = accountMap[token.accountId.toString()]
            if (account != null) {
                localAssets.add(
                    DiemCurrencyAssetVo(
                        token.id!!,
                        token.accountId,
                        account.coinNumber,
                        DiemCurrency(token.module, token.name, token.address),
                        token.enable,
                        token.amount,
                        token.logo
                    ).apply {
                        setAssetsName(token.assetsName)
                        calculateDisplayAmount(this)
                        calculateFiatAmount(this)
                    }
                )
            }
        }

        return localAssets
    }

    /**
     * 刷新资产
     */
    suspend fun refreshAssets(localAssets: MutableList<AssetVo>): MutableList<AssetVo> {
        val assets = localAssets.toMutableList()
        val queryViolasBalanceDeferred = GlobalScope.exceptionAsync { queryViolasBalance(assets) }
        val queryDiemBalanceDeferred = GlobalScope.exceptionAsync { queryDiemBalance(assets) }
        val queryBitcoinBalanceDeferred = GlobalScope.exceptionAsync { queryBitcoinBalance(assets) }

        queryViolasBalanceDeferred.await()
        queryDiemBalanceDeferred.await()
        queryBitcoinBalanceDeferred.await()
        return assets
    }

    private suspend fun queryViolasBalance(localAssets: MutableList<AssetVo>) {
        localAssets.filter {
            it is CoinAssetVo && it.getCoinNumber() == getViolasCoinType().coinNumber()
        }.forEach { diemCoinAsset ->
            diemCoinAsset as DiemCoinAssetVo

            try {
                val violasRpcService = DataRepository.getViolasRpcService()
                violasRpcService.getAccountState(diemCoinAsset.address)?.let { accountState ->
                    diemCoinAsset.authKey = accountState.authenticationKey
                    diemCoinAsset.delegatedKeyRotationCapability =
                        accountState.delegatedKeyRotationCapability
                    diemCoinAsset.delegatedWithdrawalCapability =
                        accountState.delegatedWithdrawalCapability

                    val currencyAssetMap = localAssets.filter { asset ->
                        asset is DiemCurrencyAssetVo && asset.getAccountId() == diemCoinAsset.getAccountId()
                    }.toMap { asset ->
                        (asset as DiemCurrencyAssetVo).currency.module.toUpperCase(Locale.ENGLISH)
                    }
                    accountState.balances?.forEach { accountBalance ->
                        val currencyAsset =
                            currencyAssetMap[accountBalance.currency.toUpperCase(Locale.ENGLISH)]
                        if (currencyAsset == null) {
                            localAssets.add(
                                DiemCurrencyAssetVo(
                                    id = -1L,
                                    diemCoinAsset.getAccountId(),
                                    diemCoinAsset.getCoinNumber(),
                                    DiemCurrency(accountBalance.currency),
                                    enable = false,
                                    amount = accountBalance.amount,
                                    logo = ""
                                ).apply {
                                    setAssetsName(accountBalance.currency)
                                    calculateDisplayAmount(this)
                                    calculateFiatAmount(this)
                                })
                        } else {
                            currencyAsset.setAmount(accountBalance.amount)
                            calculateDisplayAmount(currencyAsset)
                            calculateFiatAmount(currencyAsset)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    if (diemCoinAsset.authKey != null && diemCoinAsset.authKey!!.isBlank()) {
                        diemCoinAsset.authKey = null
                    }
                } catch (ignore: Exception) {
                }
            }
        }
    }

    private suspend fun queryDiemBalance(localAssets: MutableList<AssetVo>) {
        localAssets.filter {
            it is CoinAssetVo && it.getCoinNumber() == getDiemCoinType().coinNumber()
        }.forEach { diemCoinAsset ->
            diemCoinAsset as DiemCoinAssetVo

            try {
                val diemRpcService = DataRepository.getDiemRpcService()
                diemRpcService.getAccountState(diemCoinAsset.address)?.let { accountState ->
                    diemCoinAsset.authKey = accountState.authenticationKey
                    diemCoinAsset.delegatedKeyRotationCapability =
                        accountState.delegatedKeyRotationCapability
                    diemCoinAsset.delegatedWithdrawalCapability =
                        accountState.delegatedWithdrawalCapability

                    val currencyAssetMap = localAssets.filter { asset ->
                        asset is DiemCurrencyAssetVo && asset.getAccountId() == diemCoinAsset.getAccountId()
                    }.toMap { asset ->
                        (asset as DiemCurrencyAssetVo).currency.module.toUpperCase(Locale.getDefault())
                    }
                    accountState.balances?.forEach { accountBalance ->
                        val currencyAsset =
                            currencyAssetMap[accountBalance.currency.toUpperCase(Locale.getDefault())]
                        if (currencyAsset == null) {
                            localAssets.add(
                                DiemCurrencyAssetVo(
                                    id = -1L,
                                    diemCoinAsset.getAccountId(),
                                    diemCoinAsset.getCoinNumber(),
                                    DiemCurrency(accountBalance.currency),
                                    enable = false,
                                    amount = accountBalance.amount,
                                    logo = ""
                                ).apply {
                                    setAssetsName(accountBalance.currency)
                                    calculateDisplayAmount(this)
                                    calculateFiatAmount(this)
                                })
                        } else {
                            currencyAsset.setAmount(accountBalance.amount)
                            calculateDisplayAmount(currencyAsset)
                            calculateFiatAmount(currencyAsset)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    if (diemCoinAsset.authKey != null && diemCoinAsset.authKey!!.isBlank()) {
                        diemCoinAsset.authKey = null
                    }
                } catch (ignore: Exception) {
                }
            }
        }
    }

    private fun queryBitcoinBalance(localAssets: List<AssetVo>) {
        localAssets.filter {
            it is CoinAssetVo && it.getCoinNumber() == getBitcoinCoinType().coinNumber()
        }.forEach { coinAsset ->
            coinAsset as CoinAssetVo

            DataRepository.getBitcoinService()
                .getBalance(coinAsset.address)
                .subscribe({ balance ->
                    coinAsset.setAmount(balance.toLong())
                    calculateDisplayAmount(coinAsset)
                    calculateFiatAmount(coinAsset)
                }, {
                    it.printStackTrace()
                })
        }
    }

    /**
     * 刷新法币资产
     */
    suspend fun refreshFiatAssets(localAssets: MutableList<AssetVo>): MutableList<AssetVo> {
        val assets = localAssets.toMutableList()
        val queryViolasFiatBalanceDeferred = GlobalScope.exceptionAsync {
            queryFiatRate(assets) {
                it is CoinAssetVo && it.getCoinNumber() == getViolasCoinType().coinNumber()
            }
        }
        val queryDiemFiatBalanceDeferred = GlobalScope.exceptionAsync {
            queryFiatRate(assets) {
                it is CoinAssetVo && it.getCoinNumber() == getDiemCoinType().coinNumber()
            }
        }
        val queryBitcoinFiatBalanceDeferred = GlobalScope.exceptionAsync {
            queryFiatRate(assets) {
                it is CoinAssetVo && (it.getCoinNumber() == getBitcoinCoinType().coinNumber())
            }
        }

        queryViolasFiatBalanceDeferred.await()
        queryDiemFiatBalanceDeferred.await()
        queryBitcoinFiatBalanceDeferred.await()
        return assets
    }

    private suspend fun queryFiatRate(
        localAssets: List<AssetVo>,
        filter: (AssetVo) -> Boolean
    ) {
        localAssets.filter {
            filter.invoke(it)
        }.forEach { coinAsset ->
            coinAsset as CoinAssetVo

            try {
                val fiatRates = when (coinAsset.getCoinNumber()) {
                    getViolasCoinType().coinNumber() -> {
                        DataRepository.getViolasService()
                            .getViolasChainFiatRates(coinAsset.address)
                    }
                    getDiemCoinType().coinNumber() -> {
                        DataRepository.getViolasService()
                            .getDiemChainFiatRates(coinAsset.address)
                    }
                    getBitcoinCoinType().coinNumber() -> {
                        DataRepository.getViolasService()
                            .getBitcoinChainFiatRates(coinAsset.address)
                    }
                    else -> null
                }

                val fiatRateMap = fiatRates?.toMap {
                    it.name.toUpperCase(Locale.ENGLISH)
                }
                localAssets.filter {
                    it.getCoinNumber() == coinAsset.getCoinNumber()
                }.map { asset ->
                    val fiatRate = when (asset) {
                        is CoinAssetVo -> fiatRateMap?.get(
                            asset.getAssetsName().toUpperCase(Locale.ENGLISH)
                        )
                        is DiemCurrencyAssetVo -> fiatRateMap?.get(
                            asset.currency.module.toUpperCase(Locale.ENGLISH)
                        )
                    }
                    fiatRate?.let {
                        calculateFiatAmount(asset, it.rate.toString())
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    /**
     * 保存资产法定汇率
     */
    @WorkerThread
    fun saveFiatRate(localAssets: List<AssetVo>?) {
        if (localAssets.isNullOrEmpty()) return

        val edit = fiatRatesSharedPreferences.edit()
        for (asset in localAssets) {
            if (asset is CoinAssetVo && asset.accountType == AccountType.NoDollars)
                continue

            edit.putString(getFiatRateKey(asset), asset.fiatAmountWithUnit.rate)
        }
        edit.apply()
    }

    /**
     * 保存资产余额
     */
    fun saveBalance(localAssets: List<AssetVo>?) {
        if (localAssets.isNullOrEmpty()) return

        for (asset in localAssets) {
            if (asset is CoinAssetVo && asset.accountType != AccountType.NoDollars) {
                AccountManager.getAccountStorage().saveCoinBalance(
                    asset.getId(),
                    asset.getAmount()
                )
            } else if (asset is DiemCurrencyAssetVo && asset.getId() != -1L) {
                DataRepository.getTokenStorage().saveCurrencyBalance(
                    asset.getId(),
                    asset.getAmount()
                )
            }
        }
    }

    /**
     * 计算资产显示金额
     */
    private fun calculateDisplayAmount(asset: AssetVo) {
        val displayAmountWithUnit = convertAmountToDisplayUnit(
            asset.getAmount(),
            CoinType.parseCoinNumber(asset.getCoinNumber())
        )
        asset.amountWithUnit.amount = displayAmountWithUnit.first
        asset.amountWithUnit.unit = asset.getAssetsName()
    }

    /**
     * 计算资产法定金额
     */
    private fun calculateFiatAmount(asset: AssetVo, fiatRate: String = getFiatRate(asset)) {
        asset.fiatAmountWithUnit.rate = fiatRate
        try {
            if (asset.amountWithUnit.amount.toDouble() == 0.0 || fiatRate.toDouble() == 0.0) {
                asset.fiatAmountWithUnit.amount = "0.00"
            } else {
                asset.fiatAmountWithUnit.amount = BigDecimal(asset.amountWithUnit.amount)
                    .multiply(BigDecimal(fiatRate))
                    .setScale(2, RoundingMode.DOWN)
                    .stripTrailingZeros().toPlainString()
            }
        } catch (e: Exception) {
            asset.fiatAmountWithUnit.amount = "0.00"
        }
    }

    /**
     * 获取资产法定汇率
     */
    private fun getFiatRate(asset: AssetVo): String {
        return fiatRatesSharedPreferences.getString(getFiatRateKey(asset), "0.00") ?: "0.00"
    }

    /**
     * 获取资产法定汇率的key
     */
    private fun getFiatRateKey(assets: AssetVo): String {
        return when (assets) {
            is CoinAssetVo ->
                "${assets.getCoinNumber()}-${assets.getAssetsName()}"
            is DiemCurrencyAssetVo ->
                "${assets.getCoinNumber()}-${assets.currency.module}-${assets.currency.name}-${assets.currency.address}"
        }
    }
}