package com.violas.wallet.repository.http.bitcoin.trezor

import com.palliums.exceptions.RequestException
import com.palliums.net.await

/**
 * Created by elephant on 2020/6/5 18:11.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Trezor repository
 */
class BitcoinTrezorRepository(private val api: BitcoinTrezorApi) {

    @Throws(RequestException::class)
    suspend fun getTransactionRecords(
        address: String,
        pageSize: Int,
        pageNumber: Int
    ) =
        api.getTransactionRecords(address, pageSize, pageNumber).await()

}