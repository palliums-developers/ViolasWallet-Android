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
        const val BASE_URL_MAIN_NET = "http://52.27.228.84:4000"
        const val BASE_URL_TEST_NET = "http://52.27.228.84:4000"
    }

    /**
     * 获取指定地址的交易记录，分页查询
     * @param address 地址
     * @param pageSize 分页大小
     * @param offset 偏移量，从0开始
     * @param offset 稳定币地址，不为空时查询该稳定币的交易记录，为空时查询平台币的交易记录
     */
    @GET("/1.0/violas/transaction")
    suspend fun getTransactionRecord(
        @Query("addr") address: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int,
        @Query("modu") tokenAddress: String?
    ): ListResponse<TransactionRecordDTO>

    /**
     * 获取余额
     * @param address 账号地址
     * @param tokenAddressArr 稳定币地址，多个稳定币地址以逗号分开，为空时只返回平台币的余额
     */
    @GET("/1.0/violas/balance")
    fun getBalance(
        @Query("addr") address: String,
        @Query("modu") tokenAddressArr: String?
    ): Single<Response<BalanceDTO>>

    @GET("/1.0/violas/seqnum")
    fun getSequenceNumber(@Query("addr") address: String): Single<Response<Long>>

    @POST("/1.0/violas/transaction")
    fun pushTx(@Body requestBody: RequestBody): Single<Response<Any>>

    @GET("/1.0/violas/currency")
    fun getSupportCurrency(): Single<ListResponse<SupportCurrencyDTO>>

    @GET("/1.0/violas/module")
    fun getRegisterToken(@Query("addr") address: String): Single<ListResponse<String>>
}