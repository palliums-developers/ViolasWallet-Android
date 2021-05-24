package com.violas.wallet.repository.http.bitcoin.bitmain

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.palliums.net.ApiResponse
import java.math.BigInteger

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

    override fun isSuccess(): Boolean {
        return errorCode == 0
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

data class AccountStateDTO(
    val address: String = "",
    val balance: BigInteger = BigInteger.ZERO,
    val received: BigInteger = BigInteger.ZERO,
    val sent: BigInteger = BigInteger.ZERO,
    val tx_count: Int = 0,
    val unconfirmed_received: BigInteger = BigInteger.ZERO,
    val unconfirmed_sent: BigInteger = BigInteger.ZERO,
    val unconfirmed_tx_count: Int = 0,
    val unspent_tx_count: Int = 0
)

data class UTXODTO(
    val confirmations: Long = 0,
    val tx_hash: String = "",
    val tx_output_n: Int = 0,   // 未花费在交易输出中的纵向排序
    val tx_output_n2: Int = 0,  // 未花费在交易输出中的横向排序
    val value: BigInteger = BigInteger.ZERO
)

data class TransactionDTO(
    val block_hash: String = "",
    val block_height: Long = 0,
    val block_time: Long = 0,
    val confirmations: Long = 0,
    val fee: Long = 0,
    val hash: String = "",
    val inputs: List<InputDTO>? = null,
    val inputs_count: Long = 0,
    val inputs_value: Long = 0,
    val is_coinbase: Boolean = false,
    val is_double_spend: Boolean = false,
    val is_sw_tx: Boolean = false,
    val lock_time: Long = 0,
    val outputs: List<OutputDTO>? = null,
    val outputs_count: Long = 0,
    val outputs_value: Long = 0,
    val sigops: Long = 0,
    val size: Long = 0,
    val version: Int = 0,
    val vsize: Long = 0,
    val weight: Long = 0,
    val witness_hash: String = ""
) {

    data class InputDTO(
        val prev_addresses: List<String>? = null,
        val prev_position: Int = 0,
        val prev_tx_hash: String = "",
        val prev_type: String = "",
        val prev_value: BigInteger = BigInteger.ZERO,
        val script_asm: String = "",
        val script_hex: String = "",
        val sequence: Long = 0,
        val witness: List<String>? = null
    )

    data class OutputDTO(
        val addresses: List<String>? = null,
        val script_asm: String = "",
        val script_hex: String = "",
        val spent_by_tx: String = "",
        val spent_by_tx_position: Int = 0,
        val type: String = "",
        val value: BigInteger = BigInteger.ZERO
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
) {

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
}



