package com.violas.walletconnect.models.violas

open class WCViolasSendTransaction(
    val from: String,
    val payload: WCViolasSendTransactionPayload,
    val maxGasAmount: Long? = 400_000,
    val gasUnitPrice: Long? = 0,
    val sequenceNumber: Long?,
    val delayed: Long? = 1000
)

data class WCViolasSendTransactionPayload(
    val code: String,
    val tyArgs: List<String>,
    val args: List<WCViolasSendTransactionPayloadArgs>
)

data class WCViolasSendTransactionPayloadArgs(
    val type: String,
    val value: String
)