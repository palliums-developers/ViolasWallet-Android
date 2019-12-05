package com.violas.wallet.repository.http.dex

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by elephant on 2019-12-05 17:36.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易所api
 * @see <a href="https://github.com/palliums-developers/violas-dex-backend/tree/master/docs/api">link</a>
 */
interface DexApi {

    /**
     * 分页查询我的订单
     * @param accountAddress 账号地址
     * @param pageSize 分页大小
     * @param lastVersion 最后一个item的version，初始值可以为空
     * @param orderState 订单状态: 0=OPEN, 1=FILLED, 2=CANCELED, 3=FILLED and CANCELED
     * @param baseTokenAddress
     * @param quoteTokenAddress
     */
    @GET("/v1/orders")
    suspend fun getMyOrders(
        @Query("user") accountAddress: String,
        @Query("limit") pageSize: String,
        @Query("version") lastVersion: String,
        @Query("state") orderState: String? = null,
        @Query("base") baseTokenAddress: String? = null,
        @Query("quote") quoteTokenAddress: String? = null
    )
}