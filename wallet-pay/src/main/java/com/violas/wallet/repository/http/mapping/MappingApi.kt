package com.violas.wallet.repository.http.mapping

import com.palliums.violas.http.ListResponse
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by elephant on 2020-02-14 11:21.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface MappingApi {

    /**
     * 获取所有映射币种对信息
     */
    @GET("/1.0/mapping/address/info")
    fun getMappingCoinPairs(): Observable<ListResponse<MappingCoinPairDTO>>

    /**
     * 分页获取映射记录
     * @param walletAddresses
     * @param pageSize
     * @param offset
     */
    @GET("/1.0/mapping/transaction")
    fun getMappingRecords(
        @Query("addresses") walletAddresses: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<MappingRecordDTO>>
}