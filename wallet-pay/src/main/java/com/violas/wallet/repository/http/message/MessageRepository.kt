package com.violas.wallet.repository.http.message

import com.palliums.net.await
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Created by elephant on 1/25/21 2:44 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MessageRepository(private val api: MessageApi) {

    suspend fun registerDevice(
        address: String,
        token: String
    ) =
        """{
    "address":"$address",
    "token":"$token",
    "platform":"android",
    "language":"${MultiLanguageUtility.getInstance().localTag.toLowerCase()}"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            .let { api.registerDevice(it).await() }

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
        msgId: String
    ) =
        api.getTransactionMsgDetails(msgId).await(dataNullableOnSuccess = false).data!!

    suspend fun getSystemMsgDetails(
        msgId: String,
        token: String
    ) =
        api.getSystemMsgDetails(msgId, token).await(dataNullableOnSuccess = false).data!!
}