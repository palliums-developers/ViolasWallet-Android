package com.violas.walletconnect.models.session

import com.violas.walletconnect.models.WCAccount

data class WCSessionUpdate(
    val approved: Boolean,
    val chainId: String?,
    val accounts: List<WCAccount>?
)