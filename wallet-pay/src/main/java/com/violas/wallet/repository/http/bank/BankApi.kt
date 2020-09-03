package com.violas.wallet.repository.http.bank

import com.palliums.violas.http.ListResponse
import com.palliums.violas.http.Response
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by elephant on 2020/8/24 11:23.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

interface BankApi {

    /**
     * 获取账户信息
     */
    @GET("/1.0/violas/bank/account/info")
    fun getAccountInfo(
        @Query("address") address: String
    ): Observable<Response<AccountInfoDTO>>

    /**
     * 获取存款产品列表
     */
    @GET("/1.0/violas/bank/product/deposit")
    fun getDepositProducts(): Observable<ListResponse<DepositProductSummaryDTO>>

    /**
     * 获取存款产品详情
     * @param id 业务 ID
     * @param address
     */
    @GET("/1.0/violas/bank/deposit/info")
    fun getDepositProductDetails(
        @Query("id") id: String,
        @Query("address") address: String
    ): Observable<Response<DepositProductDetailsDTO>>

    /**
     * 分页获取存款信息
     */
    @GET("/1.0/violas/bank/deposit/orders")
    fun getDepositInfos(
        @Query("address") address: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<DepositInfoDTO>>

    /**
     * 获取存款详情
     */
    @GET("/1.0/violas/bank/deposit/withdrawal")
    fun getDepositDetails(
        @Query("id") id: String,
        @Query("address") address: String
    ): Observable<Response<DepositDetailsDTO>>

    /**
     * 分页获取存款记录
     * @param currency  不填查全部
     * @param state     不填查全部，0（已存款），1（已提取），-1（提取失败），-2（存款失败）
     */
    @GET("/1.0/violas/bank/deposit/order/list")
    fun getDepositRecords(
        @Query("address") address: String,
        @Query("currency") currency: String?,
        @Query("status") state: Int?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<DepositRecordDTO>>

    /**
     * 获取借贷产品列表
     */
    @GET("/1.0/violas/bank/product/borrow")
    fun getBorrowingProducts(): Observable<ListResponse<BorrowingProductSummaryDTO>>

    /**
     * 获取借贷产品详情
     * @param id
     * @param address
     */
    @GET("/1.0/violas/bank/borrow/info")
    fun getBorrowProductDetails(
        @Query("id") id: String,
        @Query("address") address: String
    ): Observable<Response<BorrowProductDetailsDTO>>

    /**
     * 分页获取借贷信息
     */
    @GET("/1.0/violas/bank/borrow/orders")
    fun getBorrowingInfos(
        @Query("address") address: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<BorrowingInfoDTO>>

    /**
     * 分页获取借贷记录
     * @param currency  不填查全部
     * @param state     不填查全部，0（已借款），1（已还款），2（已清算），-1（借款失败），-2（还款失败）
     */
    @GET("/1.0/violas/bank/borrow/order/list")
    fun getBorrowingRecords(
        @Query("address") address: String,
        @Query("currency") currency: String?,
        @Query("status") state: Int?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<BorrowingRecordDTO>>

    /**
     * 分页获取借贷明细
     */
    @GET("/1.0/violas/bank/borrow/order/detail")
    fun getBorrowingDetails(
        @Query("address") address: String,
        @Query("id") id: String,
        @Query("q") type: Int,                  // 0:借贷明细, 1:还款明细, 2: 清算明细
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Observable<Response<BorrowOrderDetailDTO>>
}