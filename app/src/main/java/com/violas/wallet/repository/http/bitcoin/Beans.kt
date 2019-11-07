package com.violas.wallet.repository.http.bitcoin

import com.violas.wallet.repository.http.BasePagingResponse

/**
 * Created by elephant on 2019-11-07 19:05.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

class TransactionRecordResponse : BasePagingResponse<TransactionRecordResponse.Bean>() {

    data class Bean(
        val balance_diff: Int,
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



