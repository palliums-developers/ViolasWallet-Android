package com.violas.wallet.biz

import android.content.Context
import android.util.ArrayMap
import com.palliums.utils.toMap
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.bean.TokenMark
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountType
import com.violas.wallet.repository.http.dex.DexOrderDTO
import com.violas.wallet.repository.http.dex.DexRepository
import com.violas.wallet.ui.main.market.bean.*
import com.violas.wallet.ui.main.quotes.bean.IToken
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import org.palliums.violascore.crypto.KeyPair
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal
import java.util.HashMap

class ExchangeManager {

    private val mAccountStorage by lazy {
        DataRepository.getAccountStorage()
    }
    private val mViolasService by lazy {
        DataRepository.getViolasService()
    }
    private val mViolasRpcService by lazy {
        DataRepository.getViolasChainRpcService()
    }
    private val mLibraRpcService by lazy {
        DataRepository.getLibraService()
    }

    private val mAccountManager by lazy {
        AccountManager()
    }
    private val mTokenManager by lazy {
        TokenManager()
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
        fromCoin: IToken,
        fromCoinAmount: BigDecimal,
        toCoin: IToken,
        toCoinAmount: BigDecimal
    ): Boolean {
        val optionExchangePayloadWithData = optionExchangePayloadWithData(
            toCoin.tokenIdx(),
            toCoinAmount.multiply(BigDecimal("1000000")).toLong()
        )
        val optionExchangePayload = mTokenManager.transferTokenPayload(
            fromCoin.tokenIdx(),
            receiveAddress,
            fromCoinAmount.multiply(BigDecimal("1000000")).toLong(),
            optionExchangePayloadWithData
        )

        try {
            mViolasService.sendTransaction(
                optionExchangePayload,
                account
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
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

    private fun mapping(): HashMap<IAssetsMark, List<IAssetsMark>> {
        val map = HashMap<IAssetsMark, List<IAssetsMark>>()

        map[LibraTokenAssetsMark(
            CoinTypes.Libra,
            "USD",
            "0000000000000000000000000a550c18",
            "USD"
        )] =
            arrayListOf(
                LibraTokenAssetsMark(
                    CoinTypes.Violas,
                    "VLSUSD",
                    "0000000000000000000000000a550c18",
                    "VLSUSD"
                ),
                LibraTokenAssetsMark(
                    CoinTypes.Violas,
                    "VLSEUR",
                    "0000000000000000000000000a550c18",
                    "VLSEUR"
                )
            )

        map[LibraTokenAssetsMark(
            CoinTypes.Violas,
            "VLS",
            "0000000000000000000000000a550c18",
            "VLS"
        )] =
            arrayListOf(
                LibraTokenAssetsMark(
                    CoinTypes.Libra,
                    "USD",
                    "0000000000000000000000000a550c18",
                    "USD"
                ),
                LibraTokenAssetsMark(
                    CoinTypes.Libra,
                    "EUR",
                    "0000000000000000000000000a550c18",
                    "EUR"
                ),
                CoinAssetsMark(
                    if (Vm.TestNet) {
                        CoinTypes.Bitcoin
                    } else {
                        CoinTypes.BitcoinTest
                    }
                )
            )

        return map
    }

    private fun findIndexListByTokenList(
        mark: IAssetsMark,
        tokens: List<ITokenVo>
    ): ArrayList<Int> {
        val indexList = ArrayList<Int>()
        tokens.forEachIndexed { index, iTokenVo ->
            if (mark is LibraTokenAssetsMark
                && iTokenVo is StableTokenVo
                && mark.coinTypes.coinType() == iTokenVo.coinNumber
                && (mark.name != iTokenVo.name && mark.address != iTokenVo.address && mark.module != iTokenVo.module)
            ) {
                indexList.add(index)
            }
        }
        return indexList
    }

    /**
     * 获取映射兑换交易 和 币币 交易支持的币种 bitmap
     */
    fun getMappingMarketSupportTokens(supportTokens: List<ITokenVo>): HashMap<String, MutBitmap> {
        val supportTokenMap = HashMap<String, Int>(supportTokens.size)
        supportTokens.forEachIndexed { index, iTokenVo ->
            supportTokenMap[IAssetsMark.convert(iTokenVo).mark()] = index
        }

        val result = HashMap<String, MutBitmap>()
        val mappingMarketSupportTokens = mapping()

        mappingMarketSupportTokens.keys.forEach { key ->
            val bitmap = MutBitmap()
            mappingMarketSupportTokens[key]?.forEach {
                supportTokenMap[it.mark()]?.let { index ->
                    bitmap.setBit(index)
                }
            }

            findIndexListByTokenList(key,supportTokens).forEach {
                bitmap.setBit(it)
            }

            result[key.mark()] = bitmap
        }

        return result
    }
}
