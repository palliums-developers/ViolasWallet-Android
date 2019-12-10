package com.violas.wallet.repository.http.dex

import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor.Companion.HEADER_KEY_URLNAME
import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor.Companion.HEADER_VALUE_DEX
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

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
     * @param lastVersion 最后一个item的version，初始值可以为空
     * @param orderState 订单状态: 0=OPEN, 1=FILLED, 2=CANCELED, 3=FILLED and CANCELED
     * @param giveTokenAddress 提供的代币地址（若拿A换B，则A为此参数）
     * @param getTokenAddress 换取的代币地址（若拿A换B，则B为此参数）
     */
    @Headers(value = ["${HEADER_KEY_URLNAME}:${HEADER_VALUE_DEX}"])
    @GET("/v1/orders")
    suspend fun getMyOrders(
        @Query("user") accountAddress: String,
        @Query("limit") pageSize: String,
        @Query("version") lastVersion: String,
        @Query("state") orderState: String? = null,
        @Query("give") giveTokenAddress: String? = null,
        @Query("get") getTokenAddress: String? = null
    ): ListResponse<DexOrderDTO>

    @Headers(value = ["${HEADER_KEY_URLNAME}:${HEADER_VALUE_DEX}"])
    @GET("/v1/prices")
    suspend fun getTokenPrices(): List<DexTokenPriceDTO>
}