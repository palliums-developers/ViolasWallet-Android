package com.palliums.violas.http

import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Created by elephant on 2019-11-11 15:34.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas bean
 * @see <a href="https://github.com/palliums-developers/violas-webservice">link</a>
 */
interface ViolasApi {

    companion object {
        const val BASE_URL_MAIN_NET = "http://52.27.228.84:4000/1.0/"
        const val BASE_URL_TEST_NET = "http://52.27.228.84:4000/1.0/"
    }

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
    ): ListResponse<TransactionRecordDTO>

    @GET("violas/balance")
    fun getBalance(
        @Query("addr") address: String,
        @Query("modu") module: String
    ): Single<Response<BalanceDTO>>

    @GET("violas/balance")
    fun getBalance(@Query("addr") address: String): Single<Response<BalanceDTO>>

    @GET("violas/seqnum")
    fun getSequenceNumber(@Query("addr") address: String): Single<Response<Long>>

    @POST("violas/transaction")
    fun pushTx(@Body requestBody: RequestBody): Single<Response<Any>>

    @GET("violas/currency")
    fun getSupportCurrency(): Single<ListResponse<SupportCurrencyDTO>>

    @GET("violas/module")
    fun checkRegisterToken(
        @Query("addr") address: String
    ): Single<Response<List<String>>>
}