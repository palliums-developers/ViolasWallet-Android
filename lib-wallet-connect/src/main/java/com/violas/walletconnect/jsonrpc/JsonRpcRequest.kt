package com.violas.walletconnect.jsonrpc

import com.violas.walletconnect.JSONRPC_VERSION
import com.violas.walletconnect.models.WCMethod

data class JsonRpcRequest<T>(
    val id: Long,
    val jsonrpc: String = JSONRPC_VERSION,
    val method: WCMethod?,
    val params: T
)