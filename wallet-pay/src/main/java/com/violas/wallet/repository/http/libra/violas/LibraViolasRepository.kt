package com.violas.wallet.repository.http.libra.violas

import com.palliums.net.RequestException
import com.palliums.net.checkResponse
import com.palliums.violas.http.Response
import retrofit2.http.Query

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
        checkResponse {
            mLibraViolasApi.getTransactionRecord(address, pageSize, offset)
        }

    @Throws(RequestException::class)
    suspend fun activateAccount(
        address: String, authKeyPrefix: String
    ): Response<Any> {
        return checkResponse {
            mLibraViolasApi.activateAccount(address, authKeyPrefix)
        }
    }
}