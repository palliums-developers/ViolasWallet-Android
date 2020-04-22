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
    maxGasAmount: Long = 400000,
    gasUnitPrice: Long = 0,
    delayed: Long = 1000
): RawTransaction {
    val rawTransaction = RawTransaction(
        AccountAddress(HexUtils.fromHex(senderAddress)),
        sequenceNumber,
        payload,
        maxGasAmount,
        gasUnitPrice,
        LBR_NAME,
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
    val moveEncode = Move.decode(context.assets.open("move/libra_peer_to_peer_with_metadata.mv"))

    val addressArgument = TransactionArgument.newAddress(address)
    val authenticationKeyPrefixArgument =
        TransactionArgument.newByteArray(authenticationKeyPrefix.hexToBytes())
    val amountArgument = TransactionArgument.newU64(amount)
    val metadataArgument = TransactionArgument.newByteArray(metaData)

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf(lbrStructTag()),
            arrayListOf(
                addressArgument,
                authenticationKeyPrefixArgument,
                amountArgument,
                metadataArgument
            )
        )
    )
}

fun TransactionPayload.Companion.optionWithDataPayload(
    context: Context,
    receiveAddress: String,
    exchangeSendAmount: Long,
    data: ByteArray
): TransactionPayload {

    val addressArgument = TransactionArgument.newAddress(receiveAddress)
    val amountArgument = TransactionArgument.newU64(exchangeSendAmount)
    val dateArgument = TransactionArgument.newByteArray(data)

    val moveEncode = Move.decode(
        context.assets.open("move/libra_peer_to_peer_with_metadata.mv")
    )

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf(lbrStructTag()),
            arrayListOf(addressArgument, amountArgument, dateArgument)
        )
    )
}

const val LBR_NAME = "LBR"

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