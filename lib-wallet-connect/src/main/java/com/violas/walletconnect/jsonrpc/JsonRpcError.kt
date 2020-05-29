package com.violas.walletconnect.jsonrpc

data class JsonRpcError(
    val code: Int,
    val message: String
) {
    companion object {
        fun accountNotActivationError(message: String) = JsonRpcError(-20001, message)
        fun accountNotControlError(message: String) = JsonRpcError(-20002, message)
        fun userRefused() = JsonRpcError(-20101, "User active rejection")
        fun transactionBroadcastFailed(message: String) = JsonRpcError(-20201, message)

        fun serverError(message: String) = JsonRpcError(-32000, message)
        fun invalidParams(message: String) = JsonRpcError(-32602, message)
        fun invalidRequest(message: String) = JsonRpcError(-32600, message)
        fun parseError(message: String) = JsonRpcError(-32700, message)
        fun methodNotFound(message: String) = JsonRpcError(-32601, message)
    }
}