package org.palliums.libracore.http

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.palliums.net.ApiResponse
import kotlin.random.Random

/**
 * Created by elephant on 2019-03-30 18:29.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

open class Response<T> : ApiResponse {

    var id: String? = null

    @SerializedName("jsonrpc")
    var jsonRPC: String? = null

    @SerializedName(value = "error")
    var errors: List<ResponseError>? = null

    @SerializedName(value = "result")
    var data: T? = null

    override fun getSuccessCode(): Any {
        return true
    }

    override fun getErrorCode(): Any {
        return errors.isNullOrEmpty()
    }

    override fun getErrorMsg(): Any? {
        return errors
    }

    override fun getResponseData(): Any? {
        return data
    }
}

@Keep
class ListResponse<T> : Response<List<T>>()

@Keep
data class ResponseError(
    var code: String,
    var data: Long,
    var message: Boolean
)

@Keep
data class RequestDTO(
    val method: String,
    val params: List<String>,
    @SerializedName("jsonrpc")
    val jsonRPC: String = "2.0",
    val id: String = Random(10000).nextInt().toString()
)

data class AccountStateDTO(
    val authenticationKey: String,
    val balance: Long,
    val sequenceNumber: Long,
    val sentEventsKey: String,
    val receivedEventsKey: String,
    val delegatedWithdrawalCapability: Boolean,
    val delegatedKeyRotationCapability: Boolean
)