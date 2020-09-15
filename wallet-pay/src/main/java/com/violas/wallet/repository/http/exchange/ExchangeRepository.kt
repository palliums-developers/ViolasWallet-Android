package com.violas.wallet.repository.http.exchange

import com.palliums.net.await

/**
 * Created by elephant on 2020-02-14 11:48.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ExchangeRepository(private val api: ExchangeApi) {

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
            "${violasWalletAddress}_violas,${libraWalletAddress}_libra,${bitcoinWalletAddress}_btc",
            pageSize,
            offset
        ).await().data ?: emptyList()

}