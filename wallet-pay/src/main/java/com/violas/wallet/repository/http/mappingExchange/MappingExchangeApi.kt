package com.violas.wallet.repository.http.mappingExchange

import com.palliums.violas.http.ListResponse
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
     * @param mappingType btc，vbtc，libra，vlibra
     */
    @GET("/1.0/crosschain/info")
    suspend fun getMappingInfo(@Query("type") mappingType: String): Response<MappingInfoDTO>

    /**
     * 获取映射兑换订单数
     * @param mappingType btc，vbtc，libra，vlibra
     * @param walletAddress
     */
    @GET("/1.0/crosschain/transactions/count")
    suspend fun getMappingExchangeOrderNumber(
        @Query("type") mappingType: String,
        @Query("address") walletAddress: String
    ): Response<Int>

    /**
     * 获取映射兑换订单
     * @param walletAddress
     * @param walletType 0: violas; 1: libra; 2: btc
     * @param pageSize
     * @param offset
     */
    @GET("/1.0/crosschain/transactions")
    suspend fun getMappingExchangeOrders(
        @Query("address") walletAddress: String,
        @Query("type") walletType: Int,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): MappingExchangeOrdersResponse
}