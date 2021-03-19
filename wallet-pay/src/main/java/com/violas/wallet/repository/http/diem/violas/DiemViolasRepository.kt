package com.violas.wallet.repository.http.diem.violas

import com.palliums.exceptions.RequestException
import com.palliums.net.await

/**
 * Created by elephant on 2019-11-11 15:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas repository
 */
class DiemViolasRepository(private val api: DiemViolasApi) {

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
    suspend fun activateWallet(
        address: String, authKeyPrefix: String
    ) =
        api.activateWallet(
            address, authKeyPrefix
        ).await()

    @Throws(RequestException::class)
    suspend fun getCurrencies() =
        api.getCurrency().await()

}