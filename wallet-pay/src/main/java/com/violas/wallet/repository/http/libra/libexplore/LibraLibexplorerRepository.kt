package com.violas.wallet.repository.http.libra.libexplore

import com.palliums.exceptions.RequestException
import com.palliums.net.checkResponse

/**
 * Created by elephant on 2019-11-08 18:04.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: LibExplorer repository
 */
class LibraLibexplorerRepository(private val api: LibraLibexplorerApi) {

    @Throws(RequestException::class)
    suspend fun getTransactionRecords(
        address: String,
        pageSize: Int,
        pageNumber: Int
    ) =
        // {"status":"0","message":"No transactions found","result":[]}
        checkResponse("0") {
            api.getTransactionRecords(address, pageSize, pageNumber)
        }
}