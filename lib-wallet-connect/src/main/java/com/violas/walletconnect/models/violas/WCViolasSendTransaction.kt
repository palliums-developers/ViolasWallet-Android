package com.violas.walletconnect.models.violas

open class WCViolasSendTransaction(
    val from: String,
    val payload: WCViolasSendTransactionPayload,
    val maxGasAmount: Long?,
    val gasUnitPrice: Long?,
    val sequenceNumber: Long?,
    val gasCurrencyCode: String?,
    val expirationTime: Long?,
    val chainId: Int
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