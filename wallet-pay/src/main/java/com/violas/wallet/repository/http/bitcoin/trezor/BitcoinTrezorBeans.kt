package com.violas.wallet.repository.http.bitcoin.trezor

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.palliums.net.ApiResponse

/**
 * Created by elephant on 4/27/21 4:56 PM.
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
    val balance: String = "",
    val totalReceived: String = "",
    val totalSent: String = "",
    val txs: Int = 0,
    val unconfirmedBalance: String = "",
    val unconfirmedTxs: Int = 0
) : Response()

data class UTXODTO(
    val confirmations: Long = 0,
    val height: Long = 0,
    val txid: String = "",
    val value: String = "",
    val vout: Int = 0
)

@Keep
data class TransactionResponse(
    val blockHash: String = "",
    val blockHeight: Long = 0,
    val blockTime: Long = 0,
    val confirmations: Long = 0,
    val fees: String = "",
    val hex: String = "",
    val txid: String = "",
    val value: String = "",
    val valueIn: String = "",
    val version: Int = 0,
    val vin: List<VinDTO>? = null,
    val vout: List<VoutDTO>? = null
) : Response() {

    data class VinDTO(
        val addresses: List<String>? = null,
        val hex: String? = null,
        val coinbase: String? = null,
        val isAddress: Boolean = false,
        val n: Int = 0,
        val sequence: Long = 0,
        val txid: String? = null,
        val value: String? = null,
        val vout: Int? = null
    )

    data class VoutDTO(
        val addresses: List<String>? = null,
        val hex: String = "",
        val isAddress: Boolean = false,
        val n: Int = 0,
        val spent: Boolean = false,
        val value: String = ""
    )
}

@Keep
data class PushTransactionResponse(
    val result: String = ""
) : Response()

@Keep
data class TransactionsResponse(
    val address: String = "",
    val balance: Long = 0,
    val unconfirmedBalance: Long = 0,
    val totalReceived: Long = 0,
    val totalSent: Long = 0,
    val txs: Long = 0,
    val unconfirmedTxs: Long = 0,
    val page: Int = 0,
    val totalPages: Int = 0,
    val itemsOnPage: Int = 0,
    val transactions: List<TransactionDTO>? = null
) : Response() {

    data class TransactionDTO(
        val blockHash: String = "",
        val blockHeight: Long = 0,
        val blockTime: Long = 0,
        val confirmations: Long = 0,
        val fees: Long = 0,
        val hex: String = "",
        val txid: String = "",
        val value: Long = 0,
        val valueIn: Long = 0,
        val version: Int = 0,
        val vin: List<VinDTO>? = null,
        val vout: List<VoutDTO>? = null
    )

    data class VinDTO(
        val addresses: List<String>? = null,
        val hex: String = "",
        val isAddress: Boolean = false,
        val n: Int = 0,
        val sequence: Long = 0,
        val txid: String = "",
        val value: Long = 0
    )

    data class VoutDTO(
        val addresses: List<String>? = null,
        val hex: String = "",
        val isAddress: Boolean = false,
        val n: Int = 0,
        val spent: Boolean = false,
        val value: Long = 0
    )
}