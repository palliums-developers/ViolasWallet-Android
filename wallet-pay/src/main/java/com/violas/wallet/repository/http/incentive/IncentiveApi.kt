package com.violas.wallet.repository.http.incentive

import com.palliums.violas.http.ListResponse
import com.palliums.violas.http.Response
import com.violas.wallet.repository.http.interceptor.RequestHeaderInterceptor.Companion.HEADER_KEY_CHAIN_NAME
import com.violas.wallet.repository.http.interceptor.RequestHeaderInterceptor.Companion.HEADER_VALUE_VIOLAS_CHAIN
import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * Created by elephant on 12/11/20 4:28 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface IncentiveApi {

    /**
     * 获取领取激励奖励结果
     */
    @GET("/1.0/violas/incentive/check/verified")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getReceiveIncentiveRewardsResults(
        @Query("address") address: String
    ): Observable<Response<ReceiveIncentiveRewardsResultsDTO>>

    /**
     * 领取激励奖励
     */
    @POST("/1.0/violas/incentive/mobile/verify")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun receiveIncentiveRewards(
        @Body body: RequestBody
    ): Observable<Response<Any>>

    /**
     * 获取邀请好友收益明细
     */
    @GET("/1.0/violas/incentive/orders/invite")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getInviteFriendsEarnings(
        @Query("address") address: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<InviteFriendsEarningDTO>>

    /**
     * 获取资金池挖矿收益明细
     */
    @GET("/1.0/violas/incentive/orders/pool")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getPoolMiningEarnings(
        @Query("address") address: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<PoolMiningEarningDTO>>

    /**
     * 获取数字银行挖矿收益明细
     */
    @GET("/1.0/violas/incentive/orders/bank")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getBankMiningEarnings(
        @Query("address") address: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<BankMiningEarningDTO>>
}