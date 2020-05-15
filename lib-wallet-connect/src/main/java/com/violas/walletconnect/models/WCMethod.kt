package com.violas.walletconnect.models

import com.google.gson.annotations.SerializedName

enum class WCMethod {
    @SerializedName("wc_sessionRequest")
    SESSION_REQUEST,

    @SerializedName("wc_sessionUpdate")
    SESSION_UPDATE,

    @SerializedName("violas_sendTransaction")
    VIOLAS_SEND_TRANSACTION,

    @SerializedName("violas_sendRawTransaction")
    VIOLAS_SEND_RAW_TRANSACTION,

    @SerializedName("violas_signTransaction")
    VIOLAS_SIGN_TRANSACTION,

    @SerializedName("violas_sign")
    VIOLAS_SIGN,

    @SerializedName("get_accounts")
    GET_ACCOUNTS,
}