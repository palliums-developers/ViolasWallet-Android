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
    "device_type":"android",
    "language":"${MultiLanguageUtility.getInstance().localTag.toLowerCase()}"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            .let { api.registerDevice(it).await() }

    suspend fun getTransactionMessages(
        address: String,
        pageSize: Int,
        offset: Int
    ) =
        api.getTransactionMessages(address, pageSize, offset).await().data ?: emptyList()

    suspend fun getSystemMessages(
        pageSize: Int,
        offset: Int
    ) =
        api.getSystemMessages(pageSize, offset).await().data ?: emptyList()

    suspend fun getTransactionMsgDetails(
        address: String,
        txnId: String
    ) =
        api.getTransactionMsgDetails(address, txnId).await(dataNullableOnSuccess = false).data!!
}