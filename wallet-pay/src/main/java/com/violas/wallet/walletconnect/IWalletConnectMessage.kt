package com.violas.wallet.walletconnect

import com.violas.walletconnect.jsonrpc.JsonRpcError

interface IWalletConnectMessage {
    fun sendErrorMessage(id: Long, result: JsonRpcError): Boolean
    fun <T> sendSuccessMessage(id: Long, result: T): Boolean
}