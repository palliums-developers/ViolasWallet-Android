package com.violas.wallet.repository.http.bitcoin

import com.google.gson.annotations.SerializedName
import com.violas.wallet.repository.http.ApiResponse

/**
 * Created by elephant on 2019-11-07 19:05.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 比特大陆bean
 */
open class BitmainResponse<T> : ApiResponse {

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
}

open class BitmainPagingResponse<T> : BitmainResponse<BitmainPagingResponse.Data<T>>() {

    data class Data<T>(
        @SerializedName(value = "pagesize")
        var pageSize: Int = 0,

        @SerializedName(value = "page")
        var pageNumber: Int = 0,

        @SerializedName(value = "list")
        var list: List<T>? = null
    )
}

class BitmainTransactionRecordResponse :
    BitmainPagingResponse<BitmainTransactionRecordResponse.Bean>() {

    data class Bean(
        val block_hash: String,
        val block_height: Int,
        val block_time: Int,
        val confirmations: Int,
        val created_at: Int,
        val fee: Int,
        val hash: String,
        val inputs: List<Input>,
        val inputs_count: Int,
        val inputs_value: Long,
        val is_coinbase: Boolean,
        val is_double_spend: Boolean,
        val is_sw_tx: Boolean,
        val lock_time: Int,
        val outputs: List<Output>,
        val outputs_count: Int,
        val outputs_value: Long,
        val sigops: Int,
        val size: Int,
        val version: Int,
        val vsize: Int,
        val weight: Int,
        val witness_hash: String
    )

    data class Input(
        val prev_addresses: List<String>,
        val prev_position: Int,
        val prev_tx_hash: String,
        val prev_type: String,
        val prev_value: Int,
        val sequence: Long
    )

    data class Output(
        val addresses: List<String>,
        val spent_by_tx: String,
        val spent_by_tx_position: Int,
        val type: String,
        val value: Long
    )
}



