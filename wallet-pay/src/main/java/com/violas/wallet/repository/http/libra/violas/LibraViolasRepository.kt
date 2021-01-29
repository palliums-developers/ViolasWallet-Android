package com.violas.wallet.repository.http.libra.violas

import com.palliums.exceptions.RequestException
import com.palliums.net.await

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
        tokenId: String?,
        pageSize: Int,
        offset: Int,
        transactionType: Int?
    ) =
        api.getTransactionRecords(
            address, tokenId, pageSize, offset, transactionType
        ).await()

    @Throws(RequestException::class)
    suspend fun activateAccount(
        address: String, authKeyPrefix: String
    ) =
        api.activateAccount(
            address, authKeyPrefix
        ).await()

    @Throws(RequestException::class)
    suspend fun getCurrencies() =
        api.getCurrency().await()

}