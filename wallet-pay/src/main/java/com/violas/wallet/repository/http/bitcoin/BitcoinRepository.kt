package com.violas.wallet.repository.http.bitcoin

import com.violas.wallet.repository.http.checkResponse

/**
 * Created by elephant on 2019-11-07 18:57.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BitcoinRepository(private val bitcoinApi: BitcoinApi) {

    companion object {
        private const val SUCCESS_CODE = 0
    }

    suspend fun getTransactionRecord(
        address: String,
        pageSize: Int,
        pageIndex: Int,
        verbose: Int = 2
    ): Result<TransactionRecordResponse> {
        return checkResponse(SUCCESS_CODE) {
            bitcoinApi.getTransactionRecord(
                address,
                pageSize,
                pageIndex,
                verbose
            )
        }
    }
}