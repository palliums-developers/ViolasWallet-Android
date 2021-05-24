package com.violas.wallet.repository.http.bitcoin.soChain

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.palliums.net.ApiResponse

/**
 * Created by elephant on 4/29/21 4:48 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

open class Response<T> : ApiResponse {

    @SerializedName(value = "status")
    var status: String? = null

    @SerializedName(value = "data")
    var data: T? = null

    override fun isSuccess(): Boolean {
        return status == "success"
    }

    override fun getErrorMsg(): Any? {
        return if (isSuccess() || data == null)
            null
        else
            GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(data)
    }

    override fun getResponseData(): Any? {
        return data
    }
}

data class AccountStateDTO(
    val network: String = "",
    val address: String = "",
    val confirmed_balance: String = "",
    val unconfirmed_balance: String = ""
)

data class UTXOsDTO(
    val network: String = "",
    val address: String = "",
    @SerializedName("txs")
    val utxos: List<UTXODTO>? = null
) {
    data class UTXODTO(
        val confirmations: Long = 0,
        val output_no: Int = 0,
        val script_asm: String = "",
        val script_hex: String = "",
        val time: Long = 0,
        val txid: String = "",
        val value: String = ""
    )
}

data class TransactionDTO(
    val network: String = "",
    val txid: String = "",
    val blockhash: String = "",
    val confirmations: Long = 0,
    val inputs: List<InputDTO>? = null,
    val locktime: Long = 0,
    val outputs: List<OutputDTO>? = null,
    val size: Long = 0,
    val time: Long = 0,
    val tx_hex: String = "",
    val version: Int? = null
) {
    data class InputDTO(
        val address: String = "",
        val from_output: FromOutputDTO? = null,
        val input_no: Long = 0,
        val script: String = "",
        val type: String = "",
        val value: String = "",
        val witness: Any? = null
    )

    data class OutputDTO(
        val address: String = "",
        val output_no: Long = 0,
        val script: String = "",
        val type: String = "",
        val value: String = ""
    )

    data class FromOutputDTO(
        val output_no: Long = 0,
        val txid: String = ""
    )
}

data class PushTransactionDTO(
    val network: String = "",
    val txid: String = ""
)