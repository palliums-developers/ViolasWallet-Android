package com.violas.walletconnect.models.violasprivate

data class WCLibraSendTransaction(
    val from: String,
    val payload: WCLibraSendTransactionPayload,
    val maxGasAmount: Long?,
    val gasUnitPrice: Long?,
    val sequenceNumber: Long?,
    val gasCurrencyCode: String?,
    val expirationTime: Long?,
    val chainId: Int
)

data class WCLibraSendTransactionPayload(
    val code: String,
    val tyArgs: List<WCLibraSendTransactionPayloadTyArg>,
    val args: List<WCLibraSendTransactionPayloadArg>
)

data class WCLibraSendTransactionPayloadTyArg(
    val module: String,
    val address: String,
    val name: String
)

data class WCLibraSendTransactionPayloadArg(
    val type: String,
    val value: String
)
