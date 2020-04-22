package org.palliums.violascore.transaction

import android.content.Context
import org.json.JSONObject
import org.palliums.violascore.move.Move
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.transaction.storage.StructTag
import org.palliums.violascore.transaction.storage.TypeTag
import org.palliums.violascore.utils.HexUtils
import java.util.*

fun lbrStructTag(): TypeTag {
    return TypeTag.newStructTag(
        StructTag(
            AccountAddress.DEFAULT,
            "LBR",
            "T",
            arrayListOf()
        )
    )
}

/**
 * 创建交易
 */
fun RawTransaction.Companion.optionTransaction(
    senderAddress: String,
    payload: TransactionPayload,
    sequenceNumber: Long,
    maxGasAmount: Long = 1_000_000,
    gasUnitPrice: Long = 0,
    delayed: Long = 1000
): RawTransaction {
    val rawTransaction = RawTransaction(
        AccountAddress(HexUtils.fromHex(senderAddress)),
        sequenceNumber,
        payload,
        maxGasAmount,
        gasUnitPrice,
        lbrStructTag(),
        (Date().time / 1000) + delayed
    )
    println("rawTransaction ${HexUtils.toHex(rawTransaction.toByteArray())}")
    return rawTransaction
}

/**
 * 创建 Token 转账 payload
 */
fun TransactionPayload.Companion.optionTransactionPayload(
    context: Context,
    address: String,
    amount: Long,
    metaData: ByteArray = byteArrayOf()
): TransactionPayload {
    val convert = AccountAddress.convert(address)
    return optionTransactionPayload(
        context,
        convert.address,
        convert.authenticationKeyPrefix,
        amount,
        metaData
    )
}

/**
 * 创建 Token 转账 payload
 */
fun TransactionPayload.Companion.optionTransactionPayload(
    context: Context,
    address: String,
    authenticationKeyPrefix: String,
    amount: Long,
    metaData: ByteArray = byteArrayOf()
): TransactionPayload {
    val moveEncode = Move.decode(context.assets.open("move/violas_peer_to_peer_with_metadata.mv"))

    val addressArgument = TransactionArgument.newAddress(address)
    val authenticationKeyPrefixArgument =
        TransactionArgument.newByteArray(authenticationKeyPrefix.hexToBytes())
    val amountArgument = TransactionArgument.newU64(amount)
    val metaDataArgument = TransactionArgument.newByteArray(metaData)

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf(lbrStructTag()),
            arrayListOf(
                addressArgument,
                authenticationKeyPrefixArgument,
                amountArgument,
                metaDataArgument
            )
        )
    )
}