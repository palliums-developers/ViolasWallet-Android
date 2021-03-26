package com.violas.wallet.walletconnect.diemTxnDataHandler

import org.palliums.libracore.transaction.TransactionPayload
import org.palliums.libracore.transaction.storage.TypeTagStructTag

fun decodeCoinName(index: Int,payload: TransactionPayload.Payload): String {
    return if (payload is TransactionPayload.Script) {
        if (payload.tyArgs.isNotEmpty() && payload.tyArgs[index] is TypeTagStructTag) {
            (payload.tyArgs[index] as TypeTagStructTag).value.module
        } else {
            ""
        }
    } else ""
}

fun decodeWithData(index: Int, payload: TransactionPayload.Payload): ByteArray {
    return if (payload is TransactionPayload.Script) {
        payload.args.getOrNull(index)?.let {
            val decodeToValue = it.decodeToValue()
            if (decodeToValue !is ByteArray) {
                byteArrayOf()
            } else {
                decodeToValue
            }
        } ?: byteArrayOf()
    } else byteArrayOf()
}