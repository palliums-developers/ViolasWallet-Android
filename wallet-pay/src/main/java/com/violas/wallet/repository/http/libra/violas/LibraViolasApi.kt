package com.violas.wallet.repository.http.libra.violas

import com.palliums.violas.http.ListResponse
import com.palliums.violas.http.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by elephant on 2019-11-11 15:34.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas libra api
 * @see <a href="https://github.com/palliums-developers/violas-webservice">link</a>
 */
interface LibraViolasApi {

    /**
     * 获取指定地址的交易记录，分页查询
     * @param address 地址
     * @param pageSize 分页大小
     * @param offset 偏移量，从0开始
     * @param offset 稳定币地址，不为空时查询该稳定币的交易记录，为空时查询平台币的交易记录
     */
    @GET("/1.0/libra/transaction")
    suspend fun getTransactionRecords(
        @Query("addr") address: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): ListResponse<TransactionRecordDTO>

    @GET("/1.0/libra/mint")
    suspend fun activateAccount(
        @Query("address") address: String,
        @Query("auth_key_perfix") authKeyPrefix: String
    ): Response<Any>
}

data class TransactionRecordDTO(
    val amount: String,
    val gas: String,
    val receiver: String?,
    val sender: String,
    val expiration_time: Long,
    val sequence_number: Long,
    val version: Long
)