package com.violas.wallet.repository.http.bitcoin.bitmain

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.palliums.net.ApiResponse

/**
 * Created by elephant on 2019-11-07 19:05.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 比特大陆bean
 */
open class Response<T> : ApiResponse {

    @SerializedName(value = "err_no")
    var errorCode: Int = 0

    @SerializedName(value = "err_msg")
    var errorMsg: String? = null

    @SerializedName(value = "data")
    var data: T? = null

    override fun getSuccessCode(): Any {
        return 0
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
class ListResponse<T> : Response<ListResponse.Data<T>>() {

    data class Data<T>(
        @SerializedName(value = "pagesize")
        var pageSize: Int = 0,

        @SerializedName(value = "page")
        var pageNumber: Int = 0,

        @SerializedName(value = "list")
        var list: List<T>? = null
    )
}

data class TransactionRecordDTO(
    val block_hash: String,
    val block_height: Long,
    val block_time: Long,
    val confirmations: Long,
    val created_at: Long,
    val fee: Long,
    val hash: String,
    val inputs: List<InputDTO>,
    val inputs_count: Int,
    val inputs_value: Long,
    val is_coinbase: Boolean,
    val is_double_spend: Boolean,
    val is_sw_tx: Boolean,
    val lock_time: Long,
    val outputs: List<OutputDTO>,
    val outputs_count: Int,
    val outputs_value: Long,
    val sigops: Int,
    val size: Long,
    val version: Long,
    val vsize: Long,
    val weight: Long,
    val witness_hash: String
)

data class InputDTO(
    val prev_addresses: List<String>,
    val prev_position: Long,
    val prev_tx_hash: String,
    val prev_type: String,
    val prev_value: Long,
    val sequence: Long
)

data class OutputDTO(
    val addresses: List<String>,
    val spent_by_tx: String,
    val spent_by_tx_position: Int,
    val type: String,
    val value: Long
)



