package com.violas.wallet.repository.http.dex

import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor.Companion.HEADER_KEY_URLNAME
import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor.Companion.HEADER_VALUE_DEX
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * Created by elephant on 2019-12-05 17:36.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易所 api
 * @see <a href="https://github.com/palliums-developers/violas-dex-backend/tree/master/docs/api">link</a>
 */
interface DexApi {

    /**
     * 分页查询我的订单
     * @param accountAddress 账号地址
     * @param pageSize 分页大小
     * @param lastVersion 最后一个item的区块高度，初始值可以为空
     * @param orderState 订单状态: 0=OPEN, 1=FILLED, 2=CANCELED, 3=FILLED and CANCELED
     * @param tokenGiveAddress 提供的代币地址（若拿A换B，则A为此参数）
     * @param tokenGetAddress 换取的代币地址（若拿A换B，则B为此参数）
     */
    @Headers(value = ["${HEADER_KEY_URLNAME}:${HEADER_VALUE_DEX}"])
    @GET("/v1/orders")
    suspend fun getMyOrders(
        @Query("user") accountAddress: String,
        @Query("limit") pageSize: Int,
        @Query("version") lastVersion: String? = null,
        @Query("state") orderState: String? = null,
        @Query("give") tokenGiveAddress: String? = null,
        @Query("get") tokenGetAddress: String? = null
    ): ListResponse<DexOrderDTO>

    @Headers(value = ["${HEADER_KEY_URLNAME}:${HEADER_VALUE_DEX}"])
    @GET("/v1/tokens")
    suspend fun getTokens(): List<DexTokenDTO>

    /**
     * 分页查询订单成交记录
     * @param version 区块高度
     * @param pageSize 分页大小
     * @param pageNumber 页码，从1开始
     */
    @Headers(value = ["${HEADER_KEY_URLNAME}:${HEADER_VALUE_DEX}"])
    @GET("/v1/trades")
    suspend fun getOrderTrades(
        @Query("version") version: String,
        @Query("pagesize") pageSize: Int,
        @Query("pagenum") pageNumber: Int
    ): ListResponse<DexOrderTradeDTO>

    /**
     * 撤销订单
     */
    @Headers(value = ["${HEADER_KEY_URLNAME}:${HEADER_VALUE_DEX}"])
    @POST("/v1/cancelOrder")
    suspend fun revokeOrder(@Body requestBody: RequestBody): Response<String>
}