package com.violas.wallet.repository.http.issuer

import com.palliums.violas.http.ListResponse
import com.palliums.violas.http.Response
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface IssuerApi {

    @GET("/1.0/violas/sso/user")
    suspend fun loadUserInfo(@Query("address") address: String): Response<UserInfoDTO>

    /**
     * 查询发行商申请发行SSO的摘要信息
     */
    @GET("/1.0/violas/sso/token/status")
    suspend fun queryApplyForIssueSSOSummary(
        @Query("address") walletAddress: String
    ): Response<ApplyForSSOSummaryDTO>

    /**
     * 获取发行商申请发行SSO的详细信息
     */
    @GET("/1.0/violas/sso/token")
    suspend fun getApplyForIssueSSODetails(
        @Query("address") walletAddress: String,
        @Query("id") ssoApplicationId: String
    ): Response<ApplyForSSODetailsDTO>

    /**
     * 申请发行稳定币
     */
    @POST("/1.0/violas/sso/token")
    suspend fun applyForIssueSSO(@Body body: RequestBody): Response<Any>

    @PUT("/1.0/violas/sso/token")
    suspend fun changePublishStatus(@Body body: RequestBody): Response<Any>

    @Multipart
    @POST("/1.0/violas/photo")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<String>

    @POST("/1.0/violas/sso/bind")
    suspend fun bind(@Body body: RequestBody): Response<Any>

    @POST("/1.0/violas/verify_code")
    suspend fun sendVerifyCode(@Body body: RequestBody): Response<Any>

    @POST("/1.0/violas/sso/user")
    suspend fun bindIdNumber(@Body body: RequestBody): Response<Any>

    @GET("/1.0/violas/sso/governors")
    suspend fun getGovernorList(@Query("offset") offset: Int, @Query("limit") limit: Int): ListResponse<GovernorDTO>
}
