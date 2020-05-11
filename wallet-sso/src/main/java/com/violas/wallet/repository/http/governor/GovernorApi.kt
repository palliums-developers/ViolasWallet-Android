package com.violas.wallet.repository.http.governor

import com.palliums.violas.http.ListResponse
import com.palliums.violas.http.Response
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * Created by elephant on 2020/2/26 17:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface GovernorApi {

    /**
     * 注册州长
     */
    @POST("/1.1/violas/governor")
    suspend fun signUpGovernor(@Body body: RequestBody): Response<Any>

    /**
     * 获取州长信息
     */
    @GET("/1.0/violas/governor/{walletAddress}")
    suspend fun getGovernorInfo(@Path("walletAddress") walletAddress: String): Response<GovernorInfoDTO>

    /**
     * 修改州长信息
     */
    @PUT("/1.0/violas/governor")
    suspend fun updateGovernorInfo(@Body body: RequestBody): Response<Any>

    /**
     * 更新州长申请状态为published
     */
    @PUT("/1.0/violas/governor/investment")
    suspend fun updateGovernorApplicationToPublished(@Body body: RequestBody): Response<Any>

    /**
     * 获取SSO申请消息
     */
    @GET("/1.0/violas/governor/token/status")
    suspend fun getSSOApplicationMsgs(
        @Query("address") walletAddress: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): ListResponse<SSOApplicationMsgDTO>

    /**
     * 获取SSO申请详情
     */
    @GET("/1.0/violas/governor/token")
    suspend fun getSSOApplicationDetails(
        @Query("address") walletAddress: String,
        @Query("id") ssoApplicationId: String
    ): Response<SSOApplicationDetailsDTO>

    /**
     * 获取审核不通过SSO申请原因列表
     */
    @GET("/1.0/violas/governor/reason")
    suspend fun getUnapproveReasons(): Response<Map<Int, String>>

    /**
     * 提交SSO申请审批结果
     */
    @PUT("/1.0/violas/governor/token/status")
    suspend fun submitSSOApplicationApprovalResults(@Body body: RequestBody): Response<Any>

    /**
     * 登录桌面端钱包
     */
    @POST("/1.0/violas/governor/singin")
    suspend fun loginDesktop(@Body body: RequestBody): Response<Any>
}