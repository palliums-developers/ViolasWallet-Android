package com.palliums.violas.http

import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.*

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
    @Headers(value = ["chainName:violas"])
    fun getBalance(
        @Query("addr") walletAddress: String
    ): Observable<Response<BalanceDTO>>

    /**
     * 获取 Violas 钱包支持的代币列表
     */
    @GET("/1.0/violas/currency")
    @Headers(value = ["chainName:violas"])
    fun getCurrency(): Observable<Response<CurrencysDTO>>

    /**
     * 获取指定地址的交易记录，分页查询
     * @param address           地址
     * @param tokenId           稳定币地址，无：全部；有：相应币种记录
     * @param pageSize          分页大小
     * @param offset            偏移量，从0开始
     * @param transactionType   交易类型，不填：全部；0：转出；1：转入
     */
    @GET("/1.0/violas/transaction")
    @Headers(value = ["chainName:violas"])
    fun getTransactionRecords(
        @Query("addr") address: String,
        @Query("currency") tokenId: String?,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int,
        @Query("flows") transactionType: Int?
    ): Observable<ListResponse<TransactionRecordDTO>>

    /**
     * 登录网页端钱包
     */
    @POST("/1.0/violas/singin")
    @Headers(value = ["chainName:violas"])
    fun loginWeb(@Body body: RequestBody): Observable<Response<Any>>

    @POST("/1.0/violas/transaction")
    @Headers(value = ["chainName:violas"])
    fun pushTx(@Body requestBody: RequestBody): Observable<Response<Any>>

    @GET("/1.0/violas/account/info")
    @Headers(value = ["chainName:violas"])
    fun getAccountState(
        @Query("address") walletAddress: String
    ): Observable<Response<AccountStateDTO>>

    @GET("/1.0/violas/mint")
    @Headers(value = ["chainName:violas"])
    fun activateWallet(
        @Query("address") address: String,
        @Query("auth_key_perfix") authKeyPrefix: String
    ): Observable<Response<Any>>

    @GET("/1.0/violas/value/violas")
    @Headers(value = ["chainName:violas"])
    fun getViolasChainFiatRates(
        @Query("address") walletAddress: String
    ): Observable<ListResponse<FiatRateDTO>>

    @GET("/1.0/violas/value/libra")
    @Headers(value = ["chainName:diem"])
    fun getDiemChainFiatRates(
        @Query("address") walletAddress: String
    ): Observable<ListResponse<FiatRateDTO>>

    @GET("/1.0/violas/value/btc")
    fun getBitcoinChainFiatRates(
        @Query("address") walletAddress: String
    ): Observable<ListResponse<FiatRateDTO>>

}