package com.smallraw.core.http.libra


data class BalanceResponse(
    var address: String = "",
    var balance: Long = 0
)

data class TransactionResponse(
    var address: String = "",
    var expiration_time: Long = 0,
    var sequence_number: Long = 0,
    var value: Long = 0,
    var version: Long = 0
)

data class SignedTxnRequest(
    var signedtxn: String = ""
)