package com.violas.wallet.biz

import com.palliums.extensions.logInfo
import com.palliums.violas.http.PoolLiquidityDTO
import com.palliums.violas.http.PoolLiquidityReserveInfoDTO
import com.palliums.violas.smartcontract.ViolasExchangeContract
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.dex.DexOrderDTO
import com.violas.wallet.repository.http.dex.DexRepository
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.PlatformTokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.wallet.utils.convertAmountToDisplayAmount
import com.violas.wallet.utils.convertDisplayAmountToAmount
import com.violas.walletconnect.extensions.hexStringToByteArray
import org.json.JSONObject
import org.palliums.violascore.crypto.KeyPair
import org.palliums.violascore.transaction.AccountAddress
import org.palliums.violascore.transaction.storage.StructTag
import org.palliums.violascore.transaction.storage.TypeTagStructTag
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal
import java.math.RoundingMode

class ExchangeManager {

    companion object {
        // 最低价格浮动汇率
        private const val MINIMUM_PRICE_FLUCTUATION = 5 / 1000F
    }

    val mViolasService by lazy {
        DataRepository.getViolasService()
    }
    private val mViolasRpcService by lazy {
        DataRepository.getViolasChainRpcService()
    }

    val mAccountManager by lazy {
        AccountManager()
    }
    private val mTokenManager by lazy {
        LibraTokenManager()
    }

    private val mViolasExchangeContract by lazy {
        ViolasExchangeContract(Vm.TestNet)
    }

    private val receiveAddress = "c71caa520e123d122c310177c08fa0d2"

    /**
     * 获取交易市场支持的币种列表
     */
    suspend fun getMarketSupportTokens(): List<ITokenVo> {
        // 交易市场支持的币种
        val marketCurrencies = mViolasService.getMarketSupportCurrencies()

        val marketTokens = mutableListOf<ITokenVo>()
        if (marketCurrencies?.bitcoinCurrencies?.isNotEmpty() == true) {
            marketTokens.add(
                PlatformTokenVo(
                    coinNumber = if (Vm.TestNet)
                        CoinTypes.BitcoinTest.coinType()
                    else
                        CoinTypes.Bitcoin.coinType(),
                    displayName = marketCurrencies.bitcoinCurrencies[0].displayName,
                    logo = marketCurrencies.bitcoinCurrencies[0].logo
                )
            )
        }
        marketCurrencies?.libraCurrencies?.forEach {
            marketTokens.add(
                StableTokenVo(
                    name = it.name,
                    module = it.module,
                    address = it.address,
                    marketIndex = it.marketIndex,
                    coinNumber = CoinTypes.Libra.coinType(),
                    displayName = it.displayName,
                    logo = it.logo
                )
            )
        }
        marketCurrencies?.violasCurrencies?.forEach {
            marketTokens.add(
                StableTokenVo(
                    name = it.name,
                    module = it.module,
                    address = it.address,
                    marketIndex = it.marketIndex,
                    coinNumber = CoinTypes.Violas.coinType(),
                    displayName = it.displayName,
                    logo = it.logo
                )
            )
        }

        return marketTokens
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
        val amountAMin =
            amountADesired - (amountADesired * MINIMUM_PRICE_FLUCTUATION).toLong()
        val amountBMin =
            amountBDesired - (amountBDesired * MINIMUM_PRICE_FLUCTUATION).toLong()

        logInfo {
            "addLiquidity. coin   a info   : module=${coinA.module}" +
                    ", index=${coinA.marketIndex}"
        }
        logInfo {
            "addLiquidity. coin   b info   : module=${coinB.module}" +
                    ", index=${coinB.marketIndex}"
        }
        logInfo { "addLiquidity. amount a desired: $amountADesired" }
        logInfo { "addLiquidity. amount b desired: $amountBDesired" }
        logInfo { "addLiquidity. amount a min    : $amountAMin" }
        logInfo { "addLiquidity. amount b min    : $amountBMin" }

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

        mViolasService.sendTransaction(
            addLiquidityTransactionPayload,
            Account(KeyPair.fromSecretKey(privateKey)),
            gasCurrencyCode = coinA.module,
            chainId = Vm.ViolasChainId
        )
    }

    suspend fun removeLiquidity(
        privateKey: ByteArray,
        coinA: PoolLiquidityDTO.CoinDTO,
        coinB: PoolLiquidityDTO.CoinDTO,
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

        val amountAMin =
            amountADesired - (amountADesired * MINIMUM_PRICE_FLUCTUATION).toLong()
        val amountBMin =
            amountBDesired - (amountBDesired * MINIMUM_PRICE_FLUCTUATION).toLong()

        logInfo {
            "removeLiquidity. coin   a info   : module=${coinA.module}" +
                    ", index=${coinA.marketIndex}"
        }
        logInfo {
            "removeLiquidity. coin   b info   : module=${coinB.module}" +
                    ", index=${coinB.marketIndex}"
        }
        logInfo { "removeLiquidity. amount a desired: $amountADesired" }
        logInfo { "removeLiquidity. amount b desired: $amountBDesired" }
        logInfo { "removeLiquidity. amount a min    : $amountAMin" }
        logInfo { "removeLiquidity. amount b min    : $amountBMin" }
        logInfo { "removeLiquidity. liquidity amount: $liquidityAmount" }

        val swapPosition = coinA.marketIndex > coinB.marketIndex
        val removeLiquidityTransactionPayload =
            mViolasExchangeContract.optionRemoveLiquidityTransactionPayload(
                if (swapPosition) typeTagB else typeTagA,
                if (swapPosition) typeTagA else typeTagB,
                liquidityAmount,
                if (swapPosition) amountBMin else amountAMin,
                if (swapPosition) amountAMin else amountBMin
            )

        mViolasService.sendTransaction(
            removeLiquidityTransactionPayload,
            Account(KeyPair.fromSecretKey(privateKey)),
            gasCurrencyCode = coinA.module,
            chainId = Vm.ViolasChainId
        )
    }

    fun estimateAddLiquidityAmount(
        inputCoinModule: String,
        inputAmountStr: String,
        liquidityReserve: PoolLiquidityReserveInfoDTO
    ): BigDecimal {
        val amountA = convertDisplayAmountToAmount(inputAmountStr)
        val reserveA = if (inputCoinModule == liquidityReserve.coinA.module)
            liquidityReserve.coinA.amount
        else
            liquidityReserve.coinB.amount
        val reserveB = if (inputCoinModule == liquidityReserve.coinA.module)
            liquidityReserve.coinB.amount
        else
            liquidityReserve.coinA.amount
        val amountB = amountA.multiply(reserveB)
            .divide(reserveA, 0, RoundingMode.DOWN)
            .toPlainString()
        return convertAmountToDisplayAmount(amountB)
    }

    fun estimateRemoveLiquidityAmounts(
        coinAModule: String,
        liquidityAmount: BigDecimal,
        liquidityReserve: PoolLiquidityReserveInfoDTO
    ): Pair<BigDecimal, BigDecimal> {
        val coinAAmount = liquidityAmount
            .multiply(
                if (coinAModule == liquidityReserve.coinA.module)
                    liquidityReserve.coinA.amount
                else
                    liquidityReserve.coinB.amount
            )
            .divide(liquidityReserve.liquidityTotalAmount, 6, RoundingMode.DOWN)
            .stripTrailingZeros()
        val coinBAmount = liquidityAmount
            .multiply(
                if (coinAModule == liquidityReserve.coinA.module)
                    liquidityReserve.coinB.amount
                else
                    liquidityReserve.coinA.amount
            )
            .divide(liquidityReserve.liquidityTotalAmount, 6, RoundingMode.DOWN)
            .stripTrailingZeros()
        return Pair(coinAAmount, coinBAmount)
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
                account,
                chainId = Vm.ViolasChainId
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


    private fun optionUndoExchangePayloadWithData(
        version: Long
    ): ByteArray {
        val subExchangeDate = JSONObject()
        subExchangeDate.put("type", "wd_ex")
        subExchangeDate.put("ver", version)
        return subExchangeDate.toString().toByteArray()
    }
}
