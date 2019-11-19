package com.violas.wallet.repository.http.violas

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by elephant on 2019-11-11 15:34.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas bean
 * @see <a href="https://github.com/palliums-developers/violas-webservice">link</a>
 */
interface ViolasApi {

    /**
     * 获取指定地址的交易记录，分页查询
     * @param address 地址
     * @param pageSize 分页大小
     * @param offset 偏移量，从0开始
     */
    @GET("violas/transaction")
    suspend fun getTransactionRecord(
        @Query("addr") address: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ):ViolasTransactionRecordResponse
}