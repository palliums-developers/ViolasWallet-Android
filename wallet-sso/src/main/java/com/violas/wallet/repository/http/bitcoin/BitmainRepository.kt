package com.violas.wallet.repository.http.bitcoin

import com.palliums.net.RequestException
import com.palliums.net.checkResponse

/**
 * Created by elephant on 2019-11-07 18:57.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 比特大陆 repository
 */
class BitmainRepository(private val mBitmainApi: BitmainApi) {

    @Throws(RequestException::class)
    suspend fun getTransactionRecord(
        address: String,
        pageSize: Int,
        pageNumber: Int
    ): ListResponse<TransactionRecordDTO> {
        return checkResponse {
            mBitmainApi.getTransactionRecord(address, pageSize, pageNumber)
        }
    }
}