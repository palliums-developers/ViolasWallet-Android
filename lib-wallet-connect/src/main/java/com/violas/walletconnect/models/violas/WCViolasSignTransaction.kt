package com.violas.walletconnect.models.violas

open class WCViolasSignTransaction(
    val from: String,
    val payload: WCViolasSignTransactionPayload,
    val maxGasAmount: Long?,
    val gasUnitPrice: Long?,
    val sequenceNumber: Long?,
    val gasCurrencyCode: String?,
    val expirationTime: Long?,
    val chainId: Int
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