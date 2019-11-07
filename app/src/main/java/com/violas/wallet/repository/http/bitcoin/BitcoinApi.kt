package com.violas.wallet.repository.http.bitcoin

import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor.Companion.HEADER_KEY_URL_NAME
import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor.Companion.HEADER_VALUE_BTC
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Created by elephant on 2019-11-07 16:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Bitcoin接口
 * @see <a href="https://btc.com/api-doc">link</a>
 */
interface BitcoinApi {

    /**
     * 获取指定地址的交易记录，分页查询
     * @param address 地址
     * @param pageSize 分页大小
     * @param pageIndex 页码，从1开始
     * @param verbose 信息输出等级，默认为2
     *                等级 1，包含交易信息；
     *                等级 2，包含等级 1，交易的输入、输出地址与金额；
     *                等级 3，包含等级 2，交易的输入、输出 script 等信息。
     */
    @Headers(value = ["${HEADER_KEY_URL_NAME}:${HEADER_VALUE_BTC}"])
    @GET("address/{address}/tx")
    suspend fun getTransactionRecord(
        @Path("address") address: String,
        @Query("pagesize") pageSize: Int,
        @Query("page") pageIndex: Int,
        @Query("verbose") verbose: Int = 2
    ): TransactionRecordResponse
}