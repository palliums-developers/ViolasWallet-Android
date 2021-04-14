package com.violas.wallet.repository.http.message

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 * Created by elephant on 12/28/20 2:56 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

@Keep
data class PushDeviceInfoDTO(
    @SerializedName("token")
    val token: String?,
    @SerializedName("address")
    val address: String?,
    @SerializedName("fcm_token")
    val pushToken: String?,
    @SerializedName("language")
    val language: String?,
    @SerializedName("platform")
    val platform: String?
)

@Keep
data class AppTokenDTO(
    @SerializedName("token")
    val token: String = ""
)

@Keep
data class UnreadMsgNumberDTO(
    @SerializedName(value = "message")
    val txn: Long = 0,
    @SerializedName(value = "notice")
    val sys: Long = 0
)

@Keep
data class TransactionMessageDTO(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("title")
    val title: String = "",
    @SerializedName("body")
    val body: String = "",
    @SerializedName("service")
    val service: String = "",
    @SerializedName("date")
    val time: Long = 0,
    @SerializedName("readed")
    var readStatus: Int = 0,

    @SerializedName("version")
    val txnId: String = "",
    @SerializedName("type")
    val txnType: String = "",
    @SerializedName("status")
    val txnStatus: String = ""
) {
    fun read(): Boolean {
        return readStatus == 1
    }

    fun markAsRead(): Boolean {
        return if (readStatus == 1) {
            false
        } else {
            readStatus = 1
            true
        }
    }
}

@Keep
data class SystemMessageDTO(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("title")
    val title: String = "",
    @SerializedName("body")
    val body: String = "",
    @SerializedName("service")
    val service: String = "",
    @SerializedName("content")
    val url: String = "",
    @SerializedName("date")
    val time: Long = 0,
    @SerializedName("readed")
    var readStatus: Int = 0
) {
    fun read(): Boolean {
        return readStatus == 1
    }

    fun markAsRead(): Boolean {
        return if (readStatus == 1) {
            false
        } else {
            readStatus = 1
            true
        }
    }
}

@Keep
data class TransactionMsgDetailsDTO(
    @SerializedName(value = "sender")
    val sender: String = "",
    @SerializedName(value = "receiver")
    val receiver: String? = null,
    @SerializedName(value = "amount")
    val amount: String = "",
    @SerializedName(value = "currency")
    val currency: String = "",
    @SerializedName(value = "gas")
    val gas: String = "",
    @SerializedName(value = "gas_currency")
    val gasCurrency: String = "",
    @SerializedName(value = "expiration_time")
    val expirationTime: Long = 0,
    @SerializedName(value = "confirmed_time")
    val confirmedTime: Long?,
    @SerializedName(value = "sequence_number")
    val sequence_number: Long = 0,
    @SerializedName(value = "version")
    val txnId: String = "",
    @SerializedName(value = "type")
    val type: String = "",
    @SerializedName(value = "status")
    val status: String = ""
)

@Keep
@Parcelize
data class SystemMsgDetailsDTO(
    @SerializedName(value = "title")
    val title: String = "",
    @SerializedName(value = "author")
    val author: String = "",
    @SerializedName(value = "content")
    val content: String = "",
    @SerializedName(value = "date")
    val time: Long = 0
) : Parcelable
