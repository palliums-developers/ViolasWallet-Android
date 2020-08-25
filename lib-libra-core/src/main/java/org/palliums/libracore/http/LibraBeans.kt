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
    val params: List<Any>,
    @SerializedName("jsonrpc")
    val jsonRPC: String = "2.0",
    val id: String = Random(10000).nextInt().toString()
)

@Keep
data class CurrenciesDTO(
    @SerializedName("code")
    val code: String,
    @SerializedName("fractional_part")
    val fractionalPart: Long,
    @SerializedName("scaling_factor")
    val scalingFactor: Long
)

@Keep
data class AccountStateDTO(
    @SerializedName("authentication_key")
    val authenticationKey: String,
    @SerializedName("balances")
    val balances: List<AccountBalance>?,
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

@Keep
data class GetTransactionDTO(
    @SerializedName("events")
    val events: List<Event>,
    @SerializedName("gas_used")
    val gasUsed: Int,
    @SerializedName("hash")
    val hash: String,
    @SerializedName("transaction")
    val transaction: Transaction,
    @SerializedName("version")
    val version: Int,
    @SerializedName("vm_status")
    val vmStatus: VmStatus
)

@Keep
data class VmStatus(
    val type: String
) {
    companion object {
        const val SUCCESS = "executed"
    }

    fun isSuccessExecuted(): Boolean {
        return type.equals(SUCCESS, true)
    }
}

@Keep
data class Event(
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("key")
    val key: String,
    @SerializedName("sequence_number")
    val sequenceNumber: Int,
    @SerializedName("transaction_version")
    val transactionVersion: Int
)

@Keep
data class Transaction(
    @SerializedName("expiration_time")
    val expirationTime: Int,
    @SerializedName("gas_currency")
    val gasCurrency: String,
    @SerializedName("gas_unit_price")
    val gasUnitPrice: Int,
    @SerializedName("max_gas_amount")
    val maxGasAmount: Int,
    @SerializedName("public_key")
    val publicKey: String,
    @SerializedName("script")
    val script: Script,
    @SerializedName("script_hash")
    val scriptHash: String,
    @SerializedName("sender")
    val sender: String,
    @SerializedName("sequence_number")
    val sequenceNumber: Int,
    @SerializedName("signature")
    val signature: String,
    @SerializedName("signature_scheme")
    val signatureScheme: String,
    @SerializedName("type")
    val type: String
)

@Keep
data class Data(
    @SerializedName("amount")
    val amount: Amount,
    @SerializedName("metadata")
    val metadata: String,
    @SerializedName("receiver")
    val `receiver`: String,
    @SerializedName("sender")
    val sender: String,
    @SerializedName("type")
    val type: String
)

@Keep
data class Amount(
    @SerializedName("amount")
    val amount: Int,
    @SerializedName("currency")
    val currency: String
)

@Keep
data class Script(
    @SerializedName("type")
    val type: String
)