package com.violas.wallet.repository.http.libra

import com.palliums.net.RequestException
import com.palliums.net.checkResponse

/**
 * Created by elephant on 2019-11-08 18:04.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: LibExplorer repository
 */
class LibexplorerRepository(private val mLibexplorerApi: LibexplorerApi) {

    @Throws(RequestException::class)
    suspend fun getTransactionRecord(
        address: String,
        pageSize: Int,
        pageNumber: Int
    ): ListResponse<TransactionRecordDTO> {
        // {"status":"0","message":"No transactions found","result":[]}
        return checkResponse("0") {
            mLibexplorerApi.getTransactionRecord(address, pageSize, pageNumber)
        }
    }
}