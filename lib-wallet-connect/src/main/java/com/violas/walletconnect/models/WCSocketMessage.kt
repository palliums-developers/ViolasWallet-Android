package com.violas.walletconnect.models

import com.violas.walletconnect.models.MessageType

data class WCSocketMessage(
    val topic: String,
    val type: MessageType,
    val payload: String
)