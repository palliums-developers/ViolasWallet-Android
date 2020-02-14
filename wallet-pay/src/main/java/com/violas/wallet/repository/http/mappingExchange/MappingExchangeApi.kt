package com.violas.wallet.repository.http.mappingExchange

import com.palliums.violas.http.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by elephant on 2020-02-14 11:21.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface MappingExchangeApi {

    /**
     * 获取映射信息
     */
    @GET("/1.0/crosschain/info")
    suspend fun getMappingInfo(@Query("type") type: String): Response<MappingInfo>

    /**
     * 获取兑换订单数
     */
    @GET("/1.0/crosschain/transactions/count")
    suspend fun getExchangeOrdersNumber(
        @Query("type") type: String,
        @Query("address") address: String
    ): Response<Int>
}