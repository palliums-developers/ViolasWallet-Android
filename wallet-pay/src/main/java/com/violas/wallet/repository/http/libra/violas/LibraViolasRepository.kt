package com.violas.wallet.repository.http.libra.violas

import com.palliums.exceptions.RequestException
import com.palliums.net.checkResponse
import com.palliums.violas.http.Response

/**
 * Created by elephant on 2019-11-11 15:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas repository
 */
class LibraViolasRepository(private val api: LibraViolasApi) {

    /**
     * 获取交易记录
     */
    @Throws(RequestException::class)
    suspend fun getTransactionRecords(
        address: String,
        pageSize: Int,
        offset: Int
    ) =
        checkResponse {
            api.getTransactionRecords(address, pageSize, offset)
        }

    @Throws(RequestException::class)
    suspend fun activateAccount(
        address: String, authKeyPrefix: String
    ): Response<Any> {
        return checkResponse {
            api.activateAccount(address, authKeyPrefix)
        }
    }
}