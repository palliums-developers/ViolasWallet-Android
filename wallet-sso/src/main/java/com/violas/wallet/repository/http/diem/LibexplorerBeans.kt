package com.violas.wallet.repository.http.diem

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.palliums.net.ApiResponse

/**
 * Created by elephant on 2019-11-11 11:56.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: LibExplorer bean
 */
open class Response<T> : ApiResponse {

    @SerializedName(value = "status")
    var errorCode: String = ""

    @SerializedName(value = "message")
    var errorMsg: String? = null

    @SerializedName(value = "result")
    var data: T? = null

    override fun isSuccess(): Boolean {
        return errorCode == "1"
    }

    override fun getErrorMsg(): Any? {
        return errorMsg
    }

    override fun getErrorCode(): Any? {
        return errorCode
    }

    override fun getResponseData(): Any? {
        return data
    }
}

@Keep
class ListResponse<T> : Response<List<T>>()

data class TransactionRecordDTO(
    val to: String,
    val from: String,
    val value: String,
    val gasUsed: Long,
    val maxGasAmount: Long,
    val gasUnitPrice: Long,
    val expirationTime: Long,
    val sequenceNumber: Int,
    val status: String,
    val version: String
)