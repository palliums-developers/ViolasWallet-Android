package com.violas.wallet.repository.http.issuer

import com.palliums.violas.http.ListResponse
import com.palliums.violas.http.Response
import io.reactivex.Observable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface IssuerApi {

    @GET("/1.0/violas/sso/user")
    fun loadUserInfo(
        @Query("address") address: String
    ): Observable<Response<UserInfoDTO>>

    /**
     * 查询发行商申请发行SSO的摘要信息
     */
    @GET("/1.0/violas/sso/token/status")
    fun queryApplyForSSOSummary(
        @Query("address") walletAddress: String
    ): Observable<Response<ApplyForSSOSummaryDTO>>

    /**
     * 获取发行商申请发行SSO的详细信息
     */
    @GET("/1.0/violas/sso/token")
    fun getApplyForSSODetails(
        @Query("address") walletAddress: String
    ): Observable<Response<ApplyForSSODetailsDTO>>

    /**
     * 申请发行稳定币
     */
    @POST("/1.0/violas/sso/token")
    fun applyForIssueToken(
        @Body body: RequestBody
    ): Observable<Response<Any>>

    /**
     * 更改申请发行SSO的状态为 published
     */
    @PUT("/1.0/violas/sso/token/status/publish")
    fun changeApplyForSSOToPublished(
        @Body body: RequestBody
    ): Observable<Response<Any>>

    @Multipart
    @POST("/1.0/violas/photo")
    fun uploadImage(
        @Part file: MultipartBody.Part
    ): Observable<Response<String>>

    @POST("/1.0/violas/sso/bind")
    fun bind(
        @Body body: RequestBody
    ): Observable<Response<Any>>

    @POST("/1.0/violas/verify_code")
    fun sendVerifyCode(
        @Body body: RequestBody
    ): Observable<Response<Any>>

    @POST("/1.0/violas/sso/user")
    fun bindIdNumber(
        @Body body: RequestBody
    ): Observable<Response<Any>>

    @GET("/1.0/violas/sso/governors")
    fun getGovernorList(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Observable<ListResponse<GovernorDTO>>

}
