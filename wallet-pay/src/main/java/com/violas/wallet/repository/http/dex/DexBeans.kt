package com.violas.wallet.repository.http.dex

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.palliums.net.ApiResponse
import kotlinx.android.parcel.Parcelize

/**
 * Created by elephant on 2019-12-05 18:29.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

open class Response<T> : ApiResponse {

    @SerializedName(value = "code")
    var errorCode: Int = 0
        get() {
            return if (errorMsg.isNullOrEmpty()) 2000 else field
        }

    @SerializedName(value = "error")
    var errorMsg: String? = null

    @SerializedName(value = "data", alternate = ["orders", "trades", "message"])
    var data: T? = null

    override fun getSuccessCode(): Any {
        return 2000
    }

    override fun getErrorMsg(): Any? {
        return errorMsg
    }

    override fun getErrorCode(): Any {
        return errorCode
    }

    override fun getResponseData(): Any? {
        return data
    }
}

@Keep
class ListResponse<T> : Response<List<T>>()

@Parcelize
data class DexOrderDTO(
    val id: String,
    val user: String,
    var state: String,
    val amountGive: String,
    @SerializedName(value = "tokenGive")
    val tokenGiveAddress: String,
    val tokenGiveSymbol: String,
    val tokenGivePrice: Double,
    val amountGet: String,
    @SerializedName(value = "tokenGet")
    val tokenGetAddress: String,
    val tokenGetSymbol: String,
    val tokenGetPrice: Double,
    val amountFilled: String,
    val version: String,
    @SerializedName(value = "update_version")
    val updateVersion: String,
    val date: Long,
    @SerializedName(value = "update_date")
    var updateDate: Long
) : Parcelable {

    fun isFinished(): Boolean {
        return state == "FILLED" || state == "CANCELED"
    }

    fun isUnfinished(): Boolean {
        return state == "OPEN" || state == "CANCELLING"
    }

    fun isCanceled(): Boolean {
        return state == "CANCELED"
    }

    fun isOpen(): Boolean {
        return state == "OPEN"
    }

    fun updateStateToRevoking(time: Long = System.currentTimeMillis()) {
        this.state = "CANCELLING"
        this.updateDate = time
    }
}

data class DexTokenDTO(
    @SerializedName(value = "addr")
    val address: String,
    val name: String
)

data class DexOrderTradeDTO(
    val version: String,
    val amount: String,
    val price: Double,
    val date: Long
)

