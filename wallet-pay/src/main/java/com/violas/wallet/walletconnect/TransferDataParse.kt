package com.violas.wallet.walletconnect

import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.storage.TypeTagStructTag

fun decodeCoinName(payload: TransactionPayload.Payload): String {
    return if (payload is TransactionPayload.Script) {
        if (payload.tyArgs.isNotEmpty() && payload.tyArgs[0] is TypeTagStructTag) {
            (payload.tyArgs[0] as TypeTagStructTag).value.module
        } else {
            ""
        }
    } else ""
}

fun decodeWithData(payload: TransactionPayload.Payload): ByteArray {
    return if (payload is TransactionPayload.Script) {
        payload.args.getOrNull(3)?.let {
            val decodeToValue = it.decodeToValue()
            if (decodeToValue !is ByteArray) {
                byteArrayOf()
            } else {
                decodeToValue
            }
        } ?: byteArrayOf()
    } else byteArrayOf()
}