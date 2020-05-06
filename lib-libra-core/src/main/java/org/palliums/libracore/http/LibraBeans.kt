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
@Keep
open class Response<T> : ApiResponse {

    var id: String? = null

    @SerializedName("jsonrpc")
    var jsonRPC: String? = null

    @SerializedName(value = "error")
    var error: ResponseError? = null

    @SerializedName(value = "result")
    var data: T? = null

    override fun getSuccessCode(): Any {
        return true
    }

    override fun getErrorCode(): Any {
        return if (error == null) true else error!!.code
    }

    override fun getErrorMsg(): Any? {
        return if (error == null) null else error!!.message
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
    var data: Any?,
    var message: String
)

@Keep
data class RequestDTO(
    val method: String,
    val params: List<String>,
    @SerializedName("jsonrpc")
    val jsonRPC: String = "2.0",
    val id: String = Random(10000).nextInt().toString()
)

@Keep
data class AccountStateDTO(
    @SerializedName("authentication_key")
    val authenticationKey: String,
    @SerializedName("balance")
    val balance: AccountBalance,
    @SerializedName("sequence_number")
    val sequenceNumber: Long,
    @SerializedName("sent_events_key")
    val sentEventsKey: String,
    @SerializedName("received_events_key")
    val receivedEventsKey: String,
    @SerializedName("delegated_withdrawal_capability")
    val delegatedWithdrawalCapability: Boolean,
    @SerializedName("delegated_key_rotation_capability")
    val delegatedKeyRotationCapability: Boolean
)

@Keep
data class AccountBalance(
    val amount: Long,
    val currency: String
)