package com.violas.wallet.repository.http.bitcoin.bitmain

import com.palliums.exceptions.RequestException
import com.palliums.net.await

/**
 * Created by elephant on 2019-11-07 18:57.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 比特大陆 repository
 */
class BitmainRepository(private val api: BitmainApi) {

    @Throws(RequestException::class)
    suspend fun getTransactionRecords(
        address: String,
        pageSize: Int,
        pageNumber: Int
    ) =
        // {"data":null,"err_no":1,"err_msg":"Resource Not Found"}
        api.getTransactionRecords(address, pageSize, pageNumber).await(1)

}