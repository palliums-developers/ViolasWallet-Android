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
     * 获取 Violas 钱包支持的代币列表
     */
    @GET("/1.0/violas/currency")
    suspend fun getCurrency(): Response<CurrencysDTO>

    /**
     * 获取指定地址的交易记录，分页查询
     * @param address           地址
     * @param tokenId           稳定币地址，无：全部；有：相应币种记录
     * @param pageSize          分页大小
     * @param offset            偏移量，从0开始
     * @param transactionType   交易类型，不填：全部；0：转出；1：转入
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
    suspend fun getAccountState(@Query("address") walletAddress: String): Response<AccountStateDTO>

    @GET("/1.0/violas/mint")
    suspend fun activateAccount(
        @Query("address") address: String,
        @Query("auth_key_perfix") authKeyPrefix: String
    ): Response<Any>

    @GET("/1.0/violas/value/btc")
    suspend fun getBTCChainFiatBalance(@Query("address") walletAddress: String): ListResponse<FiatBalanceDTO>

    @GET("/1.0/violas/value/libra")
    suspend fun getLibraChainFiatBalance(@Query("address") walletAddress: String): ListResponse<FiatBalanceDTO>

    @GET("/1.0/violas/value/violas")
    suspend fun getViolasChainFiatBalance(@Query("address") walletAddress: String): ListResponse<FiatBalanceDTO>

    /**
     * 获取交易市场支持的币种
     */
    @GET("/1.0/market/exchange/currency")
    suspend fun getMarketSupportCurrencies(): Response<MarketCurrenciesDTO>

    /**
     * 获取指定地址的交易市场兑换记录，分页查询
     * @param address       地址
     * @param pageSize      分页大小
     * @param offset        偏移量，从0开始
     */
    @GET("/1.0/market/exchange/transaction")
    suspend fun getMarketSwapRecords(
        @Query("address") address: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): ListResponse<MarketSwapRecordDTO>

    /**
     * 获取指定地址的交易市场资金池记录，分页查询
     * @param address       地址
     * @param pageSize      分页大小
     * @param offset        偏移量，从0开始
     */
    @GET("/1.0/market/pool/transaction")
    suspend fun getMarketPoolRecords(
        @Query("address") address: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): ListResponse<MarketPoolRecordDTO>

    /**
     * 获取指定地址的交易市场资金池信息
     * @param address       地址
     */
    @GET("/1.0/market/pool/info")
    suspend fun getUserPoolInfo(
        @Query("address") address: String
    ): Response<UserPoolInfoDTO>
}