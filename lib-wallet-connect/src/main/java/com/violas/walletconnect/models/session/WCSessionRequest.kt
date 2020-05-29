package com.violas.walletconnect.models.session

import com.violas.walletconnect.models.WCPeerMeta

data class WCSessionRequest(
    val peerId: String,
    val peerMeta: WCPeerMeta,
    val chainId: String?
)