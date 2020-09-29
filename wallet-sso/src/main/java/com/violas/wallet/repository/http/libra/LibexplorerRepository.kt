package com.violas.wallet.repository.http.libra

import com.palliums.exceptions.RequestException
import com.palliums.net.await

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
    ) =
        // {"status":"0","message":"No transactions found","result":[]}
        mLibexplorerApi.getTransactionRecord(
            address, pageSize, pageNumber
        ).await("0")
}