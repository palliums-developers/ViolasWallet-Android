package com.violas.wallet.biz

import com.palliums.extensions.logInfo
import com.palliums.violas.error.ViolasException
import com.palliums.violas.smartcontract.ViolasExchangeContract
import com.violas.wallet.common.*
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.exchange.PoolLiquidityDTO
import com.violas.wallet.repository.http.exchange.PoolLiquidityReserveInfoDTO
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.PlatformTokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.wallet.utils.convertAmountToDisplayAmount
import com.violas.wallet.utils.convertDisplayAmountToAmount
import com.violas.walletconnect.extensions.hexStringToByteArray
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

    val mExchangeService by lazy {
        DataRepository.getExchangeService()
    }

    private val mViolasRPCService by lazy {
        DataRepository.getViolasChainRpcService()
    }

    private val mViolasExchangeContract by lazy {
        ViolasExchangeContract(isViolasTestNet())
    }

    /**
     * 获取交易市场支持的币种列表
     */
    suspend fun getMarketSupportTokens(): List<ITokenVo> {
        // 交易市场支持的币种
        val marketCurrencies = mExchangeService.getMarketSupportCurrencies()

        val marketTokens = mutableListOf<ITokenVo>()
        if (marketCurrencies.bitcoinCurrencies?.isNotEmpty() == true) {
            marketTokens.add(
                PlatformTokenVo(
                    coinNumber = getBitcoinCoinType().coinNumber(),
                    displayName = marketCurrencies.bitcoinCurrencies[0].displayName,
                    logo = marketCurrencies.bitcoinCurrencies[0].logo
                )
            )
        }
        marketCurrencies.libraCurrencies?.forEach {
            marketTokens.add(
                StableTokenVo(
                    name = it.name,
                    module = it.module,
                    address = it.address,
                    marketIndex = it.marketIndex,
                    coinNumber = getDiemCoinType().coinNumber(),
                    displayName = it.displayName,
                    logo = it.logo
                )
            )
        }
        marketCurrencies.violasCurrencies?.forEach {
            marketTokens.add(
                StableTokenVo(
                    name = it.name,
                    module = it.module,
                    address = it.address,
                    marketIndex = it.marketIndex,
                    coinNumber = getViolasCoinType().coinNumber(),
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

        mViolasRPCService.sendTransaction(
            addLiquidityTransactionPayload,
            Account(KeyPair.fromSecretKey(privateKey)),
            gasCurrencyCode = coinA.module,
            chainId = getViolasChainId()
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

        mViolasRPCService.sendTransaction(
            removeLiquidityTransactionPayload,
            Account(KeyPair.fromSecretKey(privateKey)),
            gasCurrencyCode = coinA.module,
            chainId = getViolasChainId()
        )
    }

    /**
     * 提取挖矿奖励
     */
    @Throws(ViolasException::class)
    suspend fun withdrawReward(
        privateKey: ByteArray
    ) {
        val withdrawRewardTransactionPayload =
            mViolasExchangeContract.optionWithdrawRewardTransactionPayload()

        mViolasRPCService.sendTransaction(
            payload = withdrawRewardTransactionPayload,
            payerAccount = Account(KeyPair.fromSecretKey(privateKey)),
            chainId = getViolasChainId()
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
}
