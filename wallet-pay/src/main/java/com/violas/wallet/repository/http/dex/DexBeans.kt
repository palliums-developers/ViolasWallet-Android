package com.violas.wallet.repository.http.dex

import com.google.gson.annotations.SerializedName
import com.palliums.net.ApiResponse

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
            return if (errorMsg.isNullOrEmpty()) 200 else field
        }

    @SerializedName(value = "error")
    var errorMsg: String? = null

    @SerializedName(value = "data", alternate = ["orders"])
    var data: T? = null

    override fun getSuccessCode(): Any {
        return 200
    }

    override fun getErrorMsg(): Any? {
        return errorMsg
    }

    override fun getErrorCode(): Any {
        return errorCode
    }
}

class ListResponse<T> : Response<List<T>>()

data class DexOrderDTO(
    val amountFilled: String,
    val amountGet: String,
    val amountGive: String,
    val availableVolume: String,
    val date: String,
    val id: String,
    val state: String,
    val tokenGet: String,
    val tokenGive: String,
    val updated: String,
    val user: String,
    val version: String
)

data class DexTokenPriceDTO(
    @SerializedName(value = "addr")
    val address: String,
    val name: String,
    val price: Int
)