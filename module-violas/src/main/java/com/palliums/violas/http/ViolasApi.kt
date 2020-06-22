package com.palliums.violas.http

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

    /**
     * 获取平台币余额
     * @param walletAddress 钱包地址
     */
    @GET("/1.0/violas/balance")
    suspend fun getBalance(
        @Query("addr") walletAddress: String
    ): Response<BalanceDTO>

    /**
     * 获取指定地址的交易记录，分页查询
     * @param address           地址
     * @param tokenId           token唯一标识，null时表示查询平台币的交易记录
     * @param pageSize          分页大小
     * @param offset            偏移量，从0开始
     * @param transactionType   交易类型，null：全部；0：转出；1：转入
     */
    @GET("/1.0/violas/transaction")
    suspend fun getTransactionRecords(
        @Query("addr") address: String,
        @Query("currency") tokenId: String?,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int,
        @Query("flows") transactionType: Int?
    ): ListResponse<TransactionRecordDTO>

    /**
     * 登录网页端钱包
     */
    @POST("/1.0/violas/singin")
    suspend fun loginWeb(@Body body: RequestBody): Response<Any>

    @POST("/1.0/violas/transaction")
    suspend fun pushTx(@Body requestBody: RequestBody): Response<Any>

    @GET("/1.0/violas/account/info")
    suspend fun getAccountState(@Query("address") walletAddress: String):Response<AccountStateDTO>

    @GET("/1.0/violas/mint")
    suspend fun activateAccount(
        @Query("address") address: String,
        @Query("auth_key_perfix") authKeyPrefix: String
    ): Response<Any>
}