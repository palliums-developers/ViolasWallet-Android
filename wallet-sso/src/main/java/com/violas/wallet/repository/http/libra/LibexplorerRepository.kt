package com.violas.wallet.repository.http.libra

import com.palliums.net.NetworkException
import com.palliums.net.checkResponse

/**
 * Created by elephant on 2019-11-08 18:04.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: LibExplorer repository
 */
class LibexplorerRepository(private val mLibexplorerApi: LibexplorerApi) {

    @Throws(NetworkException::class)
    suspend fun getTransactionRecord(
        address: String,
        pageSize: Int,
        pageNumber: Int
    ): ListResponse<TransactionRecordDTO> {
        return checkResponse {
            mLibexplorerApi.getTransactionRecord(address, pageSize, pageNumber)
        }
    }
}