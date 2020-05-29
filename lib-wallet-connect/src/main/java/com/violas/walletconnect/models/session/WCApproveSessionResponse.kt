package com.violas.walletconnect.models.session

import com.violas.walletconnect.models.WCPeerMeta

data class WCApproveSessionResponse(
    val approved: Boolean = true,
    val chainId: String,
    val accounts: List<String>,
    val peerId: String?,
    val peerMeta: WCPeerMeta?
)