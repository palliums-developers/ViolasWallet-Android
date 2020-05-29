package com.violas.walletconnect

import com.violas.walletconnect.models.WCPeerMeta
import com.violas.walletconnect.models.session.WCSession
import java.util.*

data class WCSessionStoreItem(
        val session: WCSession,
        val peerId: String,
        val remotePeerId: String,
        val remotePeerMeta: WCPeerMeta,
        val isAutoSign: Boolean = false,
        val date: Date = Date()
)

interface WCSessionStore {
    var session: WCSessionStoreItem?
}