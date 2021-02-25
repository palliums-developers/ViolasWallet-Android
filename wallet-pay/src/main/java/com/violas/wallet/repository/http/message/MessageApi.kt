package com.violas.wallet.repository.http.message

import com.palliums.violas.http.ListResponse
import com.palliums.violas.http.Response
import com.violas.wallet.repository.http.interceptor.RequestHeaderInterceptor.Companion.HEADER_KEY_CHAIN_NAME
import com.violas.wallet.repository.http.interceptor.RequestHeaderInterceptor.Companion.HEADER_VALUE_VIOLAS_CHAIN
import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * Created by elephant on 1/25/21 11:41 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 通知消息Api
 */
interface MessageApi {

    /**
     * 注册设备信息
     */
    @POST("/1.0/violas/device/info")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun registerPushDeviceInfo(
        @Body body: RequestBody
    ): Observable<Response<AppTokenDTO>>

    /**
     * 修改设备信息
     */
    @PUT("/1.0/violas/device/info")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun modifyPushDeviceInfo(
        @Body body: RequestBody
    ): Observable<Response<Any>>

    /**
     * 获取未读消息数
     */
    @GET("/1.0/violas/messages/unread/count")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getUnreadMsgNumber(
        @Query("token") token: String
    ): Observable<Response<UnreadMsgNumberDTO>>

    /**
     * 清除所有未读消息
     */
    @PUT("/1.0/violas/messages/readall")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun clearUnreadMessages(
        @Body body: RequestBody
    ): Observable<Response<Any>>

    /**
     * 获取交易消息
     */
    @GET("/1.0/violas/message/transfers")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getTransactionMessages(
        @Query("address") address: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<TransactionMessageDTO>>

    /**
     * 获取系统通知消息
     */
    @GET("/1.0/violas/message/notices")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getSystemMessages(
        @Query("token") token: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<SystemMessageDTO>>

    /**
     * 获取交易消息详情
     */
    @GET("/1.0/violas/message/transfer")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getTransactionMsgDetails(
        @Query("address") token: String,
        @Query("msg_id") msgId: String
    ): Observable<Response<TransactionMsgDetailsDTO>>

    /**
     * 获取系统消息详情
     */
    @GET("/1.0/violas/message/notice")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getSystemMsgDetails(
        @Query("token") token: String,
        @Query("msg_id") msgId: String
    ): Observable<Response<SystemMsgDetailsDTO>>

}