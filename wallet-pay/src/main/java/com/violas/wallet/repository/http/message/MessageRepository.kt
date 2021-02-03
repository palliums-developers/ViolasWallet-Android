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

    suspend fun registerPushDevice(
        address: String,
        token: String
    ) =
        """{
    "address":"$address",
    "token":"$token",
    "device_type":"android",
    "language":"${MultiLanguageUtility.getInstance().localTag.toLowerCase()}"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            .let { api.registerPushDevice(it).await() }

    suspend fun getTransactionMessages(
        address: String,
        pageSize: Int,
        offset: Int
    ) =
        api.getTransactionMessages(address, pageSize, offset).await().data ?: emptyList()

    suspend fun getSystemMessages(
        appToken: String,
        pageSize: Int,
        offset: Int
    ) =
        api.getSystemMessages(appToken, pageSize, offset).await().data ?: emptyList()

    suspend fun getTransactionMsgDetails(
        address: String,
        txnId: String
    ) =
        api.getTransactionMsgDetails(address, txnId).await(dataNullableOnSuccess = false).data!!

    suspend fun getTransactionMsgDetails(
        msgId: String
    ) =
        api.getSystemMsgDetails(msgId).await(dataNullableOnSuccess = false).data!!
}