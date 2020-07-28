package com.violas.wallet.ui.main.market

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.palliums.base.BaseViewModel
import com.palliums.utils.toMap
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountType
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.PlatformTokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.wallet.utils.convertAmountToDisplayAmount
import kotlinx.coroutines.*
import java.math.BigDecimal

/**
 * Created by elephant on 2020/7/8 13:00.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易市场的ViewModel
 */
class MarketViewModel : BaseViewModel() {

    private val accountStorage by lazy {
        DataRepository.getAccountStorage()
    }
    private val tokenStorage by lazy {
        DataRepository.getTokenStorage()
    }
    private val accountManager by lazy {
        AccountManager()
    }
    private val violasService by lazy {
        DataRepository.getViolasService()
    }
    private val violasRpcService by lazy {
        DataRepository.getViolasChainRpcService()
    }
    private val libraRpcService by lazy {
        DataRepository.getLibraService()
    }

    private var marketCoinsLiveData = MutableLiveData<List<ITokenVo>?>()

    fun getMarketSupportCoinsLiveData(): LiveData<List<ITokenVo>?> {
        synchronized(lock) {
            if (marketCoinsLiveData.value == null) {
                marketCoinsLiveData = MutableLiveData()
            }
            return marketCoinsLiveData
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        try {
            loadMarketSupportCoins(if (params.isNotEmpty()) params[0] as Boolean else false)
        } catch (e: Exception) {
            marketCoinsLiveData.postValue(null)
            throw e
        }
    }

    private suspend fun loadMarketSupportCoins(
        onlyNeedViolasCoins: Boolean
    ) = coroutineScope {
        // 获取用户本地账户
        val bitcoinNumber =
            if (Vm.TestNet) CoinTypes.BitcoinTest.coinType() else CoinTypes.Bitcoin.coinType()
        val bitcoinAccount = accountStorage.findByCoinType(bitcoinNumber)
        val libraAccount = accountStorage.findByCoinType(CoinTypes.Libra.coinType())
        val violasAccount = accountStorage.findByCoinType(CoinTypes.Violas.coinType())

        // 获取用户本地币种信息
        val libraLocalTokens = libraAccount?.let {
            tokenStorage.findByAccountId(it.id).toMap { item -> item.module }
        }
        val violasLocalTokens = violasAccount?.let {
            tokenStorage.findByAccountId(it.id).toMap { item -> item.module }
        }

        // 获取交易市场支持的币种，内存有缓存则不请求
        var marketLocalCoins = marketCoinsLiveData.value
        if (marketLocalCoins.isNullOrEmpty()) {
            val marketRemoteCoins = violasService.getMarketSupportCurrencies()
            marketLocalCoins = mutableListOf()
            if (marketRemoteCoins?.bitcoinCurrencies?.isNotEmpty() == true) {
                marketLocalCoins.add(
                    PlatformTokenVo(
                        accountDoId = bitcoinAccount?.id ?: -1,
                        accountType = bitcoinAccount?.accountType ?: AccountType.Normal,
                        accountAddress = bitcoinAccount?.address ?: "",
                        coinNumber = bitcoinAccount?.coinNumber ?: bitcoinNumber,
                        displayName = marketRemoteCoins.bitcoinCurrencies[0].displayName,
                        logo = marketRemoteCoins.bitcoinCurrencies[0].logo,
                        displayAmount = BigDecimal(0)
                    )
                )
            }
            marketRemoteCoins?.libraCurrencies?.forEach {
                marketLocalCoins.add(
                    StableTokenVo(
                        accountDoId = libraAccount?.id ?: -1,
                        coinNumber = CoinTypes.Libra.coinType(),
                        marketIndex = it.marketIndex,
                        tokenDoId = libraLocalTokens?.get(it.module)?.id ?: -1,
                        address = it.address,
                        module = it.module,
                        name = it.name,
                        displayName = it.displayName,
                        logo = it.logo,
                        localEnable = libraLocalTokens?.get(it.module)?.enable ?: false,
                        chainEnable = false,
                        displayAmount = BigDecimal(0)
                    )
                )
            }
            marketRemoteCoins?.violasCurrencies?.forEach {
                marketLocalCoins.add(
                    StableTokenVo(
                        accountDoId = violasAccount?.id ?: -1,
                        coinNumber = CoinTypes.Violas.coinType(),
                        marketIndex = it.marketIndex,
                        tokenDoId = violasLocalTokens?.get(it.module)?.id ?: -1,
                        address = it.address,
                        module = it.module,
                        name = it.name,
                        displayName = it.displayName,
                        logo = it.logo,
                        localEnable = violasLocalTokens?.get(it.module)?.enable ?: false,
                        chainEnable = false,
                        displayAmount = BigDecimal(0)
                    )
                )
            }

            withContext(Dispatchers.Main) {
                marketCoinsLiveData.value = marketLocalCoins
            }
        }

        // 获取用户链上币种信息，若只需要Violas coins则不请求bitcoin和libra链上币种信息
        val violasAccountStateDeferred =
            violasAccount?.let { async { violasRpcService.getAccountState(it.address) } }
        val bitcoinBalanceDeferred = if (onlyNeedViolasCoins)
            null
        else
            bitcoinAccount?.let { async { accountManager.getBalance(it) } }
        val libraAccountStateDeferred = if (onlyNeedViolasCoins)
            null
        else
            libraAccount?.let { async { libraRpcService.getAccountState(it.address) } }

        // 开始同步余额请求，请求异常时可以忽略
        val bitcoinBalance = try {
            bitcoinBalanceDeferred?.await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        val libraRemoteTokens = try {
            libraAccountStateDeferred?.await()?.balances?.associate { it.currency to it.amount }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        val violasRemoteTokens = try {
            violasAccountStateDeferred?.await()?.balances?.associate { it.currency to it.amount }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        marketLocalCoins.forEach {
            if (it is StableTokenVo) {
                if (it.coinNumber == CoinTypes.Violas.coinType()) {
                    it.accountDoId = violasAccount?.id ?: -1
                    it.tokenDoId = violasLocalTokens?.get(it.module)?.id ?: -1
                    it.localEnable = violasLocalTokens?.get(it.module)?.enable ?: false
                    it.chainEnable = violasRemoteTokens?.containsKey(it.module) ?: false
                    it.displayAmount = convertAmountToDisplayAmount(
                        violasRemoteTokens?.get(it.module) ?: 0
                    )
                } else {
                    it.accountDoId = libraAccount?.id ?: -1
                    it.tokenDoId = libraLocalTokens?.get(it.module)?.id ?: -1
                    it.localEnable = libraLocalTokens?.get(it.module)?.enable ?: false
                    it.chainEnable = libraRemoteTokens?.containsKey(it.module) ?: false
                    it.displayAmount = convertAmountToDisplayAmount(
                        libraRemoteTokens?.get(it.module) ?: 0
                    )
                }
            } else if (it is PlatformTokenVo) {
                it.accountDoId = bitcoinAccount?.id ?: -1
                it.accountAddress = bitcoinAccount?.address ?: ""
                it.displayAmount = convertAmountToDisplayAmount(
                    bitcoinBalance ?: 0,
                    CoinTypes.parseCoinType(bitcoinNumber)
                )
            }
        }

        withContext(Dispatchers.Main) {
            marketCoinsLiveData.value = marketLocalCoins
        }
    }

    private var mockData = 0
    private suspend fun mockGetSupportTokens() {
        // test code
        if (mockData == 0) {
            //加载失败
            mockData = 1
            delay(200)
            marketCoinsLiveData.postValue(null)
            return
        }

        mockData = 0
        delay(2000)
        val vls = PlatformTokenVo(
            accountDoId = 0,
            accountType = AccountType.Normal,
            accountAddress = "00000000000000000000000000000000",
            coinNumber = CoinTypes.Violas.coinType(),
            displayName = "VLS",
            logo = "",
            displayAmount = BigDecimal(100_000000),
            anchorValue = 1.00,
            selected = false
        )

        val vlsusd = StableTokenVo(
            accountDoId = 0,
            coinNumber = CoinTypes.Violas.coinType(),
            marketIndex = 0,
            tokenDoId = 0,
            address = "00000000000000000000000000000000",
            module = "VLSUSD",
            name = "VLSUSD",
            displayName = "VLSUSD",
            logo = "",
            localEnable = true,
            chainEnable = true,
            displayAmount = BigDecimal(200_000000),
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
            logo = "",
            localEnable = true,
            chainEnable = true,
            displayAmount = BigDecimal(300_000000),
            anchorValue = 1.2526,
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
            logo = "",
            localEnable = true,
            chainEnable = true,
            displayAmount = BigDecimal(400_000000),
            anchorValue = 1.1272,
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
            logo = "",
            localEnable = true,
            chainEnable = true,
            displayAmount = BigDecimal(500_000000),
            anchorValue = 0.7167,
            selected = false
        )

        val vlsjpy = StableTokenVo(
            accountDoId = 0,
            coinNumber = CoinTypes.Violas.coinType(),
            marketIndex = 0,
            tokenDoId = 0,
            address = "00000000000000000000000000000000",
            module = "VLSJPY",
            name = "VLSJPY",
            displayName = "VLSJPY",
            logo = "",
            localEnable = true,
            chainEnable = false,
            displayAmount = BigDecimal(500_000000),
            anchorValue = 0.0093,
            selected = false
        )

        val lbr = PlatformTokenVo(
            accountDoId = 0,
            accountType = AccountType.Normal,
            accountAddress = "00000000000000000000000000000000",
            coinNumber = CoinTypes.Libra.coinType(),
            displayName = "LBR",
            logo = "",
            displayAmount = BigDecimal(100_000000),
            anchorValue = 1.00,
            selected = false
        )

        val btc = PlatformTokenVo(
            accountDoId = 0,
            accountType = AccountType.Normal,
            accountAddress = "00000000000000000000000000000000",
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