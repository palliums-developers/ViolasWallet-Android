package com.violas.wallet.repository.http.sso

import com.palliums.violas.http.Response
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface SSOApi {
    @GET("violas/sso/user")
    suspend fun loadUserInfo(@Query("address") address: String): Response<UserInfoDTO>

    @POST("violas/sso/token")
    suspend fun applyForIssuing(@Body body: RequestBody): Response<Any>

    @GET("violas/sso/token")
    suspend fun selectApplyForStatus(@Query("address") address: String): Response<ApplyForStatusDTO>

    @PUT("violas/sso/token")
    suspend fun changePublishStatus(@Body body: RequestBody): Response<Any>

    @Multipart
    @POST("violas/photo")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<String>

    @POST("violas/sso/bind")
    suspend fun bind(@Body body: RequestBody): Response<Any>

    @POST("violas/verify_code")
    suspend fun sendVerifyCode(@Body body: RequestBody): Response<Any>

    @POST("violas/sso/user")
    suspend fun bindIdNumber(@Body body: RequestBody): Response<Any>
}
