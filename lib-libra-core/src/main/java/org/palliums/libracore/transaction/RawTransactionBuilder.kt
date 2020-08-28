package org.palliums.libracore.transaction

import android.content.Context
import org.palliums.libracore.move.Move
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.transaction.storage.StructTag
import org.palliums.libracore.transaction.storage.TypeTag
import org.palliums.libracore.utils.HexUtils
import java.util.*

/**
 * 创建交易
 */
fun RawTransaction.Companion.optionTransaction(
    senderAddress: String,
    payload: TransactionPayload,
    sequenceNumber: Long,
    gasCurrencyCode: String = lbrStructTagType(),
    maxGasAmount: Long = 1_000_000,
    gasUnitPrice: Long = 0,
    delayed: Long = 600,
    chainId: Int
): RawTransaction {
    val rawTransaction = RawTransaction(
        AccountAddress(HexUtils.fromHex(senderAddress)),
        sequenceNumber,
        payload,
        maxGasAmount,
        gasUnitPrice,
        gasCurrencyCode,
        (Date().time / 1000) + delayed,
        chainId
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
    metaData: ByteArray = byteArrayOf(),
    metadataSignature: ByteArray = byteArrayOf(),
    typeTag: TypeTag = lbrStructTag()
): TransactionPayload {
    val moveEncode = Move.decode(context.assets.open("move/libra_peer_to_peer_with_metadata.mv"))

    val addressArgument = TransactionArgument.newAddress(address)
    val amountArgument = TransactionArgument.newU64(amount)
    val metadataArgument = TransactionArgument.newByteArray(metaData)
    val metadataSignatureArgument = TransactionArgument.newByteArray(metadataSignature)

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf(typeTag),
            arrayListOf(
                addressArgument,
                amountArgument,
                metadataArgument,
                metadataSignatureArgument
            )
        )
    )
}

fun TransactionPayload.Companion.optionAddCurrencyPayload(
    context: Context,
    typeTag: TypeTag = lbrStructTag()
): TransactionPayload {
    val moveEncode = Move.decode(context.assets.open("move/libra_add_currency_to_account.mv"))

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf(typeTag),
            arrayListOf()
        )
    )
}

fun lbrStructTagType(): String {
    return "LBR"
}

fun lbrStructTag(): TypeTag {
    return TypeTag.newStructTag(
        StructTag(
            AccountAddress.DEFAULT,
            "LBR",
            "LBR",
            arrayListOf()
        )
    )
}