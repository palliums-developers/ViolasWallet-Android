package com.violas.wallet.repository.http.libra.violas

import com.palliums.exceptions.RequestException
import com.palliums.net.await

/**
 * Created by elephant on 2019-11-11 15:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas repository
 */
class LibraViolasRepository(private val mLibraViolasApi: LibraViolasApi) {

    /**
     * 获取交易记录
     */
    @Throws(RequestException::class)
    suspend fun getTransactionRecord(
        address: String,
        pageSize: Int,
        offset: Int
    ) =
        mLibraViolasApi.getTransactionRecord(
            address, pageSize, offset
        ).await()

    @Throws(RequestException::class)
    suspend fun activateAccount(
        address: String, authKeyPrefix: String
    ) =
        mLibraViolasApi.activateAccount(
            address, authKeyPrefix
        ).await()

}