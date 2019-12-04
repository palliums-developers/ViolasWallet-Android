package com.violas.wallet.repository.http.sso

import com.palliums.violas.http.Response
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface SSOApi {
    @GET("/1.0/violas/sso/user")
    suspend fun loadUserInfo(@Query("address") address: String): Response<UserInfoDTO>

    @POST("/1.0/violas/sso/token")
    suspend fun applyForIssuing(@Body body: RequestBody): Response<Any>

    @GET("/1.0/violas/sso/token")
    suspend fun selectApplyForStatus(@Query("address") address: String): Response<ApplyForStatusDTO>

    @PUT("/1.0/violas/sso/token")
    suspend fun selectPublishStatus(@Body body: RequestBody): Response<Any>

    @Multipart
    @POST("/1.0/violas/photo")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<String>

    @POST("/1.0/violas/sso/bind")
    suspend fun bind(@Body body: RequestBody): Response<Any>

    @POST("/1.0/violas/verify_code")
    suspend fun sendVerifyCode(@Body body: RequestBody): Response<Any>

    @POST("/1.0/violas/sso/user")
    suspend fun bindIdNumber(@Body body: RequestBody): Response<Any>
}
