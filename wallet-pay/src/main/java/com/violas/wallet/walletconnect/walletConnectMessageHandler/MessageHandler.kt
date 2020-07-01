package com.violas.wallet.walletconnect.walletConnectMessageHandler

import com.violas.walletconnect.jsonrpc.JsonRpcError

abstract class MessageHandler(private val iMessageHandler: IWalletConnectMessage) {
    abstract suspend fun handler(requestID: Long, tx: Any): TransactionSwapVo?

    fun sendInvalidParameterErrorMessage(id: Long, msg: String) {
        iMessageHandler.sendErrorMessage(
            id,
            JsonRpcError.invalidParams("Invalid Parameter:$msg")
        )
    }
}