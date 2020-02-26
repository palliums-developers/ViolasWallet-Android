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
    @POST("/1.0/violas/governor/sso")
    suspend fun signUpGovernor(@Body body: RequestBody): Response<Any>

    /**
     * 获取州长信息
     */
    @GET("/1.0/violas/governor/{walletAddress}")
    suspend fun getGovernorInfo(@Path("walletAddress") walletAddress: String): Response<GovernorInfoDTO>

    /**
     * 获取vstake address
     */
    @GET("/1.0/violas/governor/vstake/address")
    suspend fun getVStakeAddress(): Response<String>

    /**
     * 获取SSO发币申请
     */
    @GET("/1.0/violas/sso/token/approval")
    suspend fun getSSOApplications(
        @Query("address") walletAddress: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): ListResponse<SSOApplicationDTO>

    /**
     * 审批SSO发币申请
     */
    @PUT("/1.0/violas/governor/sso")
    suspend fun approvalSSOApplication(@Body body: RequestBody): Response<Any>

    /**
     * 改变SSO发币申请为已铸币
     */
    @PUT("/1.0/violas/sso/token/minted")
    suspend fun changeSSOApplicationToMinted(@Body body: RequestBody): Response<Any>
}