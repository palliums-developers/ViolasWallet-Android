package com.violas.walletconnect.models.violas

open class WCViolasSignTransaction(
    val from: String,
    val payload: WCViolasSignTransaction,
    val maxGasAmount: Long? = 400_000,
    val gasUnitPrice: Long? = 0,
    val sequenceNumber: Long?,
    val delayed: Long? = 1000
)

data class WCViolasSignTransactionPayload(
    val code: String,
    val tyArgs: List<String>,
    val args: List<WCViolasSignTransactionPayloadArgs>
)

data class WCViolasSignTransactionPayloadArgs(
    val type: String,
    val value: String
)