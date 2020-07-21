package com.violas.wallet.biz

import android.content.Context
import com.palliums.utils.toMap
import com.palliums.violas.smartcontract.ViolasExchangeContract
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountType
import com.violas.wallet.repository.http.dex.DexOrderDTO
import com.violas.wallet.repository.http.dex.DexRepository
import com.violas.wallet.ui.main.market.bean.*
import com.violas.walletconnect.extensions.hexStringToByteArray
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import org.palliums.violascore.crypto.KeyPair
import org.palliums.violascore.transaction.AccountAddress
import org.palliums.violascore.transaction.storage.StructTag
import org.palliums.violascore.transaction.storage.TypeTagStructTag
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal

class ExchangeManager {

    companion object {
        // 最低价格浮动汇率
        private const val MINIMUM_PRICE_FLUCTUATION = 5 / 1000
    }

    private val mAccountStorage by lazy {
        DataRepository.getAccountStorage()
    }
    val mViolasService by lazy {
        DataRepository.getViolasService()
    }
    private val mViolasRpcService by lazy {
        DataRepository.getViolasChainRpcService()
    }
    private val mLibraRpcService by lazy {
        DataRepository.getLibraService()
    }

    val mAccountManager by lazy {
        AccountManager()
    }
    private val mTokenManager by lazy {
        TokenManager()
    }
    private val mViolasExchangeContract by lazy {
        ViolasExchangeContract(Vm.TestNet)
    }

    private val receiveAddress = "c71caa520e123d122c310177c08fa0d2"

    /**
     * 获取交易市场支持的币种列表
     */
    suspend fun getMarketSupportTokens(): List<ITokenVo> {
        return coroutineScope {
            // 交易市场支持的币种
            val marketCurrenciesDeferred = async {
                mViolasService.getMarketSupportCurrencies()
            }

            // 用户bitcoin链上币种信息
            val bitcoinNumber =
                if (Vm.TestNet) CoinTypes.BitcoinTest.coinType() else CoinTypes.Bitcoin.coinType()
            val bitcoinAccount = mAccountStorage.findByCoinTypeByIdentity(bitcoinNumber)
            val bitcoinBalanceDeferred = bitcoinAccount?.let {
                async { mAccountManager.getBalance(it) }
            }

            // 用户libra链上币种信息
            val libraAccount = mAccountStorage.findByCoinTypeByIdentity(
                CoinTypes.Libra.coinType()
            )
            val libraAccountStateDeferred = libraAccount?.let {
                async { mLibraRpcService.getAccountState(it.address) }
            }

            // 用户violas链上币种信息
            val violasAccount = mAccountStorage.findByCoinTypeByIdentity(
                CoinTypes.Violas.coinType()
            )
            val violasAccountStateDeferred = violasAccount?.let {
                async { mViolasRpcService.getAccountState(it.address) }
            }

            val marketCurrencies = marketCurrenciesDeferred.await()
            val bitcoinBalance = bitcoinBalanceDeferred?.await()
            val libraRemoteTokens =
                libraAccountStateDeferred?.await()?.balances?.associate {
                    it.currency to it.amount
                }
            val violasRemoteTokens =
                violasAccountStateDeferred?.await()?.balances?.associate {
                    it.currency to it.amount
                }

            // 用户libra本地币种信息
            val libraLocalTokens = libraAccount?.let {
                mTokenManager.loadTokensByAccountId(it.id).toMap { item -> item.module }
            }
            // 用户violas本地币种信息
            val violasLocalTokens = violasAccount?.let {
                mTokenManager.loadTokensByAccountId(it.id).toMap { item -> item.module }
            }

            val marketTokens = mutableListOf<ITokenVo>()
            marketCurrencies?.bitcoinCurrencies?.forEach {
                marketTokens.add(
                    PlatformTokenVo(
                        accountDoId = bitcoinAccount?.id ?: -1,
                        accountType = bitcoinAccount?.accountType ?: AccountType.Normal,
                        accountAddress = bitcoinAccount?.address ?: "",
                        coinNumber = bitcoinAccount?.coinNumber ?: bitcoinNumber,
                        displayName = it.displayName,
                        logo = it.logo,
                        amount = bitcoinBalance ?: 0
                    )
                )
            }
            marketCurrencies?.libraCurrencies?.forEach {
                marketTokens.add(
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
                        chainEnable = libraRemoteTokens?.containsKey(it.module) ?: false,
                        amount = libraRemoteTokens?.get(it.module) ?: 0
                    )
                )
            }
            marketCurrencies?.violasCurrencies?.forEach {
                marketTokens.add(
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
                        chainEnable = violasRemoteTokens?.containsKey(it.module) ?: false,
                        amount = violasRemoteTokens?.get(it.module) ?: 0
                    )
                )
            }

            return@coroutineScope marketTokens
        }
    }

    suspend fun addLiquidity(
        privateKey: ByteArray,
        coinA: StableTokenVo,
        coinB: StableTokenVo,
        amountADesired: Long,
        amountBDesired: Long
    ) {
        val typeTagA = TypeTagStructTag(
            StructTag(
                AccountAddress(coinA.address.hexStringToByteArray()),
                coinA.module,
                coinA.name,
                arrayListOf()
            )
        )
        val typeTagB = TypeTagStructTag(
            StructTag(
                AccountAddress(coinB.address.hexStringToByteArray()),
                coinB.module,
                coinB.name,
                arrayListOf()
            )
        )
        val amountAMin = amountADesired - amountADesired * MINIMUM_PRICE_FLUCTUATION
        val amountBMin = amountBDesired - amountBDesired * MINIMUM_PRICE_FLUCTUATION

        val swapPosition = coinA.marketIndex > coinB.marketIndex
        val addLiquidityTransactionPayload =
            mViolasExchangeContract.optionAddLiquidityTransactionPayload(
                if (swapPosition) typeTagB else typeTagA,
                if (swapPosition) typeTagA else typeTagB,
                if (swapPosition) amountBDesired else amountADesired,
                if (swapPosition) amountADesired else amountBDesired,
                if (swapPosition) amountBMin else amountAMin,
                if (swapPosition) amountAMin else amountBMin
            )

        mViolasRpcService.sendTransaction(
            addLiquidityTransactionPayload,
            Account(KeyPair.fromSecretKey(privateKey)),
            gasCurrencyCode = coinA.module
        )
    }

    suspend fun removeLiquidity(
        privateKey: ByteArray,
        coinA: StableTokenVo,
        coinB: StableTokenVo,
        amountADesired: Long,
        amountBDesired: Long,
        liquidityAmount: Long
    ) {
        val typeTagA = TypeTagStructTag(
            StructTag(
                AccountAddress(coinA.address.hexStringToByteArray()),
                coinA.module,
                coinA.name,
                arrayListOf()
            )
        )
        val typeTagB = TypeTagStructTag(
            StructTag(
                AccountAddress(coinB.address.hexStringToByteArray()),
                coinB.module,
                coinB.name,
                arrayListOf()
            )
        )

        val amountAMin = amountADesired - amountADesired * MINIMUM_PRICE_FLUCTUATION
        val amountBMin = amountBDesired - amountBDesired * MINIMUM_PRICE_FLUCTUATION

        val swapPosition = coinA.marketIndex > coinB.marketIndex
        val addLiquidityTransactionPayload =
            mViolasExchangeContract.optionRemoveLiquidityTransactionPayload(
                if (swapPosition) typeTagB else typeTagA,
                if (swapPosition) typeTagA else typeTagB,
                liquidityAmount,
                if (swapPosition) amountBMin else amountAMin,
                if (swapPosition) amountAMin else amountBMin
            )

        mViolasRpcService.sendTransaction(
            addLiquidityTransactionPayload,
            Account(KeyPair.fromSecretKey(privateKey)),
            gasCurrencyCode = coinA.module
        )
    }

    @Throws(Exception::class)
    suspend fun revokeOrder(
        privateKey: ByteArray,
        dexOrder: DexOrderDTO,
        dexService: DexRepository
    ): Boolean {
        return try {
            // 1.获取撤销兑换token数据的签名字符
            val account = Account(KeyPair.fromSecretKey(privateKey))

            val optionUndoExchangePayloadWithData =
                optionUndoExchangePayloadWithData(dexOrder.version.toLong())

            val optionUndoExchangePayload = mTokenManager.transferTokenPayload(
                dexOrder.tokenGiveAddress.toLong(),
                receiveAddress,
                0,
                optionUndoExchangePayloadWithData
            )

            val (signedTxn, _, _) = mViolasService.generateTransaction(
                optionUndoExchangePayload,
                account
            )

            // 2.通知交易中心撤销订单，交易中心此时只会标记需要撤销订单的状态为CANCELLING并停止兑换，失败会抛异常
            // 不管通知交易中心撤销订单有没有成功，都要将撤销兑换token数据上链
            try {
                dexService.revokeOrder(dexOrder.version, signedTxn)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3.撤销兑换token数据上链，只有上链后，交易中心扫区块扫到解析撤销订单才会更改订单状态为CANCELED
            mViolasService.sendTransaction(signedTxn)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exchangeToken(
        context: Context,
        account: Account,
        fromCoin: ITokenVo,
        fromCoinAmount: BigDecimal,
        toCoin: ITokenVo,
        toCoinAmount: BigDecimal
    ): Boolean {
//        val optionExchangePayloadWithData = optionExchangePayloadWithData(
//            toCoin.tokenIdx(),
//            toCoinAmount.multiply(BigDecimal("1000000")).toLong()
//        )
//        val optionExchangePayload = mTokenManager.transferTokenPayload(
//            fromCoin.tokenIdx(),
//            receiveAddress,
//            fromCoinAmount.multiply(BigDecimal("1000000")).toLong(),
//            optionExchangePayloadWithData
//        )
//
//        try {
//            mViolasService.sendTransaction(
//                optionExchangePayload,
//                account
//            )
//        } catch (e: Exception) {
//            e.printStackTrace()
//            return false
//        }
        return true
    }

    private fun optionExchangePayloadWithData(
        exchangeTokenIdx: Long,
        exchangeReceiveAmount: Long
    ): ByteArray {
        val subExchangeDate = JSONObject()
        subExchangeDate.put("type", "sub_ex")
        subExchangeDate.put("tokenid", exchangeTokenIdx)
        subExchangeDate.put("amount", exchangeReceiveAmount)
        subExchangeDate.put("fee", 0)
        subExchangeDate.put("exp", 1000)
        return subExchangeDate.toString().toByteArray()
    }

    private fun optionUndoExchangePayloadWithData(
        version: Long
    ): ByteArray {
        val subExchangeDate = JSONObject()
        subExchangeDate.put("type", "wd_ex")
        subExchangeDate.put("ver", version)
        return subExchangeDate.toString().toByteArray()
    }
}
