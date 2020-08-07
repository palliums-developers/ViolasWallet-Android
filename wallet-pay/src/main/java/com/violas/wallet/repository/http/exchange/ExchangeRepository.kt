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
        walletAddress: String,
        pageSize: Int,
        offset: Int
    ) =
        api.getSwapRecords(walletAddress, pageSize, offset).await().data

    suspend fun getCrossChainSwapRecords(
        walletAddress: String, chainName: String, pageSize: Int, offset: Int
    ) =
        api.getCrossChainSwapRecords(walletAddress, chainName, pageSize, offset).await().data

}