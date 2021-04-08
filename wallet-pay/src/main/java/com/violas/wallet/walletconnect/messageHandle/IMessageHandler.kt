package com.violas.wallet.walletconnect.messageHandle

import com.violas.wallet.walletconnect.TransactionSwapVo
import com.violas.walletconnect.jsonrpc.JsonRpcError
import com.violas.walletconnect.models.WCMethod

/**
 * 内部处理过的异常
 */
class ProcessedRuntimeException(msg: String = "") : RuntimeException(msg)

class InvalidParameterErrorMessage(val requestID: Long, msg: String) : Exception(msg)
class WalletConnectErrorMessage(val requestID: Long, val msg: JsonRpcError) : Exception()

interface IMessageHandler<T> {

    fun canHandle(method: WCMethod): Boolean

    fun decodeMessage(id: Long, param: T): TransactionSwapVo?

    fun decodeViewData(param: Any) {

    }
}