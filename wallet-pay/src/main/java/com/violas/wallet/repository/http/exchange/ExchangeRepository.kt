package com.violas.wallet.repository.http.exchange

import com.palliums.exceptions.RequestException
import com.palliums.net.await

/**
 * Created by elephant on 2020-02-14 11:48.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ExchangeRepository(private val api: ExchangeApi) {

    @Throws(RequestException::class)
    suspend fun getMarketSupportCurrencies() = run {
        val violasCurrencies = api.getMarketSupportCurrencies().await().data
        MarketCurrenciesDTO(violasCurrencies = violasCurrencies)
    }

    @Throws(RequestException::class)
    suspend fun exchangeSwapTrial(
        amount: Long,
        currencyIn: String,
        currencyOut: String
    ) =
        api.exchangeSwapTrial(amount, currencyIn, currencyOut).await().data

    @Throws(RequestException::class)
    suspend fun getUserPoolInfo(
        address: String
    ) =
        api.getUserPoolInfo(address).await().data

    @Throws(RequestException::class)
    suspend fun removePoolLiquidityEstimate(
        address: String,
        tokenAName: String,
        tokenBName: String,
        liquidityAmount: String
    ) =
        api.removePoolLiquidityEstimate(
            address,
            tokenAName,
            tokenBName,
            liquidityAmount
        ).await(dataNullableOnSuccess = false).data!!

    @Throws(RequestException::class)
    suspend fun addPoolLiquidityEstimate(
        tokenAName: String,
        tokenBName: String,
        tokenAAmount: String
    ) =
        api.addPoolLiquidityEstimate(
            tokenAName,
            tokenBName,
            tokenAAmount
        ).await(dataNullableOnSuccess = false).data

    @Throws(RequestException::class)
    suspend fun getPoolLiquidityReserve(
        coinAModule: String,
        coinBModule: String
    ) =
        api.getPoolLiquidityReserve(
            coinAModule,
            coinBModule
        ).await(4000).data

    @Throws(RequestException::class)
    suspend fun getMarketMappingPairInfo() =
        api.getMarketMappingPairInfo().await().data

    @Throws(RequestException::class)
    suspend fun getMarketPairRelation() =
        api.getMarketPairRelation().await().data

    @Throws(RequestException::class)
    suspend fun getMarketAllReservePair() =
        api.getMarketAllReservePair().await().data

    suspend fun getPoolRecords(
        walletAddress: String,
        pageSize: Int,
        offset: Int
    ) =
        api.getPoolRecords(walletAddress, pageSize, offset).await().data

    suspend fun getSwapRecords(
        violasWalletAddress: String,
        libraWalletAddress: String,
        bitcoinWalletAddress: String,
        pageSize: Int,
        offset: Int
    ) =
        api.getSwapRecords(
            "${violasWalletAddress}_violas,${
                libraWalletAddress
            }_libra,${
                bitcoinWalletAddress
            }_btc",
            pageSize,
            offset
        ).await().data ?: emptyList()

}