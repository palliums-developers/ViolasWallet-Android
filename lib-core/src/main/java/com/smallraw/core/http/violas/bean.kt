package com.smallraw.core.http.violas


data class BalanceResponse(
    var address: String = "",
    var balance: Long = 0,
    var modules: List<Module>? = null
)

data class Module(
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

data class SupportCurrencyResponse(
    var description: String = "",
    var address: String = "",
    var name: String = ""
)