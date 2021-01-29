package com.violas.wallet.repository.http.message

import com.palliums.violas.http.ListResponse
import com.palliums.violas.http.Response
import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Created by elephant on 1/25/21 11:41 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 通知消息Api
 */
interface MessageApi {

    /**
     * 注册推送设备信息
     */
    @POST("/1.0/violas/device/info")
    fun registerPushDevice(
        @Body body: RequestBody
    ): Observable<Response<Any>>

    /**
     * 获取交易消息
     */
    @GET("/1.0/violas/messages")
    fun getTransactionMessages(
        @Query("address") address: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<TransactionMessageDTO>>

    /**
     * 获取系统通知消息
     */
    @GET("/1.0/violas/notifications")
    fun getSystemMessages(
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<SystemMessageDTO>>

    /**
     * 获取交易消息详情
     */
    @GET("/1.0/violas/message/content")
    fun getTransactionMsgDetails(
        @Query("address") address: String,
        @Query("version") txnId: String
    ): Observable<Response<TransactionMsgDetailsDTO>>
}