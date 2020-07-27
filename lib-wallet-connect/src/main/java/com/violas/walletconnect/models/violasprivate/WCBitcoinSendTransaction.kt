package com.violas.walletconnect.models.violasprivate

import com.google.gson.annotations.SerializedName

data class WCBitcoinSendTransaction(
    @SerializedName("amount")
    val amount: Long,
    @SerializedName("changeAddress")
    val changeAddress: String,
    @SerializedName("from")
    val from: String,
    @SerializedName("payeeAddress")
    val payeeAddress: String,
    @SerializedName("script")
    val script: String?
)