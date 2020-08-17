package com.violas.wallet.repository.http.exchange

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
interface ExchangeApi {

    /**
     * 获取交易市场资金池记录
     * @param walletAddress 地址
     * @param pageSize      分页大小
     * @param offset        偏移量，从0开始
     */
    @GET("/1.0/market/pool/transaction")
    fun getPoolRecords(
        @Query("address") walletAddress: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<PoolRecordDTO>>

    /**
     * 获取交易市场兑换记录
     * @param walletAddress 地址
     * @param pageSize      分页大小
     * @param offset        偏移量，从0开始
     */
    @GET("/1.0/market/exchange/transaction")
    fun getSwapRecords(
        @Query("address") walletAddress: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<SwapRecordDTO>>

    /**
     * 获取交易市场跨链兑换记录
     * @param walletAddress 地址
     * @param chainName     libra、btc、violas
     * @param pageSize      分页大小
     * @param offset        偏移量，从0开始
     */
    @GET("/1.0/market/crosschain/transaction")
    fun getCrossChainSwapRecords(
        @Query("address") walletAddress: String,
        @Query("chain") chainName: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<CrossChainSwapRecordDTO>>
}