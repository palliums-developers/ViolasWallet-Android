package com.violas.wallet.repository.http.message

import com.google.gson.Gson
import com.palliums.net.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Created by elephant on 1/25/21 2:44 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MessageRepository(private val api: MessageApi) {

    suspend fun registerPushDeviceInfo(
        address: String?,
        pushToken: String?,
        language: String
    ) =
        Gson().toJson(
            PushDeviceInfoDTO(
                token = null,
                address = address,
                pushToken = pushToken,
                language = language,
                platform = "android"
            )
        ).toRequestBody("application/json".toMediaTypeOrNull())
            .let {
                api.registerPushDeviceInfo(it).await(dataNullableOnSuccess = false).data!!.token
            }

    suspend fun modifyPushDeviceInfo(
        token: String,
        address: String?,
        pushToken: String?,
        language: String
    ) =
        Gson().toJson(
            PushDeviceInfoDTO(
                token = token,
                address = address,
                pushToken = pushToken,
                language = language,
                platform = "android"
            )
        ).toRequestBody("application/json".toMediaTypeOrNull())
            .let { api.modifyPushDeviceInfo(it).await() }

    suspend fun getUnreadMsgNumber(
        token: String
    ) =
        api.getUnreadMsgNumber(token).await().data ?: UnreadMsgNumberDTO(0, 0)

    suspend fun clearUnreadMessages(
        token: String
    ) =
        """{
    "token":"$token"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            .let { api.clearUnreadMessages(it).await() }

    suspend fun getTransactionMessages(
        address: String,
        pageSize: Int,
        offset: Int
    ) =
        api.getTransactionMessages(address, pageSize, offset).await().data ?: emptyList()

    suspend fun getSystemMessages(
        token: String,
        pageSize: Int,
        offset: Int
    ) =
        api.getSystemMessages(token, pageSize, offset).await().data ?: emptyList()

    suspend fun getTransactionMsgDetails(
        address: String,
        msgId: String
    ) =
        api.getTransactionMsgDetails(address, msgId).await(dataNullableOnSuccess = false).data!!

    suspend fun getSystemMsgDetails(
        token: String,
        msgId: String
    ) =
        api.getSystemMsgDetails(token, msgId).await(dataNullableOnSuccess = false).data!!
}