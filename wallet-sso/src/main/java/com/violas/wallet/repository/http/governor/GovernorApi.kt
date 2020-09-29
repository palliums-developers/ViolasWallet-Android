package com.violas.wallet.repository.http.governor

import com.palliums.violas.http.ListResponse
import com.palliums.violas.http.Response
import io.reactivex.Observable
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
    fun signUpGovernor(
        @Body body: RequestBody
    ): Observable<Response<Any>>

    /**
     * 获取州长信息
     */
    @GET("/1.0/violas/governor/{walletAddress}")
    fun getGovernorInfo(
        @Path("walletAddress") walletAddress: String
    ): Observable<Response<GovernorInfoDTO>>

    /**
     * 修改州长信息
     */
    @PUT("/1.0/violas/governor")
    fun updateGovernorInfo(
        @Body body: RequestBody
    ): Observable<Response<Any>>

    /**
     * 更改申请州长的状态为 published
     */
    @PUT("/1.0/violas/governor/status/published")
    fun changeApplyForGovernorToPublished(
        @Body body: RequestBody
    ): Observable<Response<Any>>

    /**
     * 获取SSO申请消息
     */
    @GET("/1.0/violas/governor/token/status")
    fun getSSOApplicationMsgs(
        @Query("address") walletAddress: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<SSOApplicationMsgDTO>>

    /**
     * 获取SSO申请详情
     */
    @GET("/1.0/violas/governor/token")
    fun getSSOApplicationDetails(
        @Query("address") walletAddress: String,
        @Query("id") ssoApplicationId: String
    ): Observable<Response<SSOApplicationDetailsDTO>>

    /**
     * 获取审核不通过SSO申请原因列表
     */
    @GET("/1.0/violas/governor/reason")
    fun getUnapproveReasons(): Observable<Response<Map<Int, String>>>

    /**
     * 提交SSO申请审批结果
     */
    @PUT("/1.0/violas/governor/token/status")
    fun submitSSOApplicationApprovalResults(
        @Body body: RequestBody
    ): Observable<Response<Any>>

    /**
     * 登录桌面端钱包
     */
    @POST("/1.0/violas/governor/singin")
    fun loginDesktop(
        @Body body: RequestBody
    ): Observable<Response<Any>>

}