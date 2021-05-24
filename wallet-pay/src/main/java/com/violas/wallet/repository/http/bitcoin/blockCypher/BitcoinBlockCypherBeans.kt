package com.violas.wallet.repository.http.bitcoin.blockCypher

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.palliums.net.ApiResponse
import java.math.BigInteger

/**
 * Created by elephant on 4/27/21 4:49 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

@Keep
open class Response : ApiResponse {

    @SerializedName(value = "error")
    var errorMsg: String? = null

    override fun isSuccess(): Boolean {
        return errorMsg.isNullOrBlank()
    }

    override fun getErrorMsg(): Any? {
        return errorMsg
    }
}

@Keep
data class AccountStateResponse(
    val address: String = "",
    val balance: BigInteger = BigInteger.ZERO,
    val unconfirmed_balance: BigInteger = BigInteger.ZERO,
    val final_balance: BigInteger = BigInteger.ZERO,
    val total_received: BigInteger = BigInteger.ZERO,
    val total_sent: BigInteger = BigInteger.ZERO,
    val n_tx: Int = 0,
    val unconfirmed_n_tx: Int = 0,
    val final_n_tx: Int = 0
) : Response()

@Keep
data class UTXOsResponse(
    val address: String = "",
    val balance: BigInteger = BigInteger.ZERO,
    val unconfirmed_balance: BigInteger = BigInteger.ZERO,
    val final_balance: BigInteger = BigInteger.ZERO,
    val total_received: BigInteger = BigInteger.ZERO,
    val total_sent: BigInteger = BigInteger.ZERO,
    val final_n_tx: Int = 0,
    val unconfirmed_n_tx: Int = 0,
    val n_tx: Int = 0,
    val tx_url: String = "",
    @SerializedName("txrefs")
    val utxos: List<UTXODTO>? = null
) : Response() {

    data class UTXODTO(
        val block_height: Long = 0,
        val confirmations: Long = 0,
        val confirmed: String = "",
        val double_spend: Boolean = false,
        val ref_balance: BigInteger? = null,
        val script: String = "",
        val spent: Boolean = false,
        val tx_hash: String = "",
        val tx_input_n: Int = 0,
        val tx_output_n: Int = 0,
        val value: BigInteger = BigInteger.ZERO
    )
}

@Keep
data class TransactionResponse(
    val addresses: List<String>? = null,
    val block_hash: String = "",
    val hex: String = "",
    val block_height: Long = 0,
    val block_index: Int = 0,
    val confidence: Int = 0,
    val confirmations: Long = 0,
    val confirmed: String = "",
    val double_spend: Boolean = false,
    val fees: Long = 0,
    val hash: String = "",
    val inputs: List<InputDTO>? = null,
    val opt_in_rbf: Boolean? = null,
    val outputs: List<OutputDTO>? = null,
    val preference: String = "",
    val received: String = "",
    val relayed_by: String? = null,
    val size: Long = 0,
    val total: Long = 0,
    val ver: Int = 0,
    val vin_sz: Long = 0,
    val vout_sz: Long = 0,
    val vsize: Long = 0
) {

    data class InputDTO(
        val addresses: List<String>? = null,
        val age: Int = 0,
        val output_index: Int = 0,
        val output_value: BigInteger? = null,
        val prev_hash: String? = null,
        val script: String = "",
        val script_type: String = "",
        val sequence: Long = 0
    )

    data class OutputDTO(
        val addresses: List<String>? = null,
        val script: String = "",
        val script_type: String = "",
        val spent_by: String = "",
        val value: BigInteger = BigInteger.ZERO
    )
}

@Keep
data class PushTransactionResponse(
    val addresses: List<String>? = null,
    val block_height: Long? = null,
    val confirmations: Long? = null,
    val double_spend: Boolean? = null,
    val fees: Long? = null,
    val hash: String? = null,
    val inputs: List<InputDTO>? = null,
    val lock_time: Long? = null,
    val outputs: List<OutputDTO>? = null,
    val preference: String? = null,
    val received: String? = null,
    val relayed_by: String? = null,
    val size: Long? = null,
    val total: Long? = null,
    val ver: Long? = null,
    val vin_sz: Long? = null,
    val vout_sz: Long? = null,
    val vsize: Long? = null
) : Response() {

    data class InputDTO(
        val addresses: List<String>? = null,
        val age: Long? = null,
        val output_index: Long? = null,
        val output_value: Long? = null,
        val prev_hash: String? = null,
        val script: String? = null,
        val script_type: String? = null,
        val sequence: Long? = null
    )

    data class OutputDTO(
        val addresses: List<String>? = null,
        val script: String? = null,
        val script_type: String? = null,
        val value: Long? = null
    )
}

@Keep
data class ChainStateResponse(
    val hash: String = "",
    val height: Long = 0,
    val last_fork_hash: String = "",
    val last_fork_height: Long = 0,
    val latest_url: String = "",
    val high_fee_per_kb: Long = 0,
    val low_fee_per_kb: Long = 0,
    val medium_fee_per_kb: Long = 0,
    val name: String = "",
    val peer_count: Long = 0,
    val previous_hash: String = "",
    val previous_url: String = "",
    val time: String = "",
    val unconfirmed_count: Long = 0
) : Response()