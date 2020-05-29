package com.violas.walletconnect.models

data class WCEncryptionPayload(
    val data: String,
    val hmac: String,
    val iv: String
)