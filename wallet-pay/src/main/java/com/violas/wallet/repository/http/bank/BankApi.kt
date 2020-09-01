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
     * 账户信息
     */
    @GET("/1.0/violas/bank/account/info")
    fun getUserBankInfo(
        @Query("address") address: String
    ): Observable<Response<UserBankInfoDTO>>

    /**
     * 获取存款产品信息
     * @param id 业务 ID
     * @param address
     */
    @GET("/1.0/violas/bank/deposit/info")
    fun getDepositProductDetails(
        @Query("id") id: String,
        @Query("address") address: String
    ): Observable<DepositProductDetailsDTO>

    /**
     * 获取账户存款信息
     */
    @GET("/1.0/violas/bank/deposit/orders")
    fun getAccountDepositInfos(
        @Query("address") address: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<AccountDepositInfoDTO>>

    /**
     * 获取存款订单列表
     */
    @GET("/1.0/violas/bank/deposit/order/list")
    fun getDepositOrderList(
        @Query("address") address: String,
        @Query("currency") currency: String,
        @Query("status") status: Int,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Observable<ListResponse<DepositOrderDTO>>

    /**
     * 获取借贷产品信息
     * @param id
     * @param address
     */
    @GET("/1.0/violas/bank/borrow/info")
    fun getBorrowProductDetails(
        @Query("id") id: String,
        @Query("address") address: String
    ): Observable<BorrowProductDetailsDTO>

    /**
     * 获取账户借贷信息
     */
    @GET("/1.0/violas/bank/borrow/orders")
    fun getAccountBorrowingInfos(
        @Query("address") address: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<AccountBorrowingInfoDTO>>

    /**
     * 获取借贷订单列表
     */
    @GET("/1.0/violas/bank/borrow/order/list")
    fun getBorrowOrderList(
        @Query("address") address: String,
        @Query("currency") currency: String,
        @Query("status") status: Int,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Observable<ListResponse<BorrowOrderDTO>>

    /**
     * 获取借贷订单详情
     */
    @GET("/1.0/violas/bank/borrow/order/detail")
    fun getBorrowDetail(
        @Query("address") address: String,
        @Query("id") id: String,
        @Query("q") q: Int,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Observable<BorrowOrderDetailDTO>
}