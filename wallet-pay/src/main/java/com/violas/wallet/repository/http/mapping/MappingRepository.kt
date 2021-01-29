package com.violas.wallet.repository.http.mapping

import com.palliums.net.await

/**
 * Created by elephant on 2020-02-14 11:48.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MappingRepository(private val api: MappingApi) {

    suspend fun getMappingCoinPairs() =
        api.getMappingCoinPairs().await().data

    suspend fun getMappingRecords(
        violasWalletAddress: String,
        libraWalletAddress: String,
        bitcoinWalletAddress: String,
        pageSize: Int,
        offset: Int
    ) =
        api.getMappingRecords(
            "${violasWalletAddress}_violas,${libraWalletAddress}_libra,${bitcoinWalletAddress}_btc",
            pageSize,
            offset
        ).await().data

}