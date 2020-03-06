package org.palliums.libracore.transaction

import android.content.Context
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.utils.HexUtils
import java.util.*

/**
 * 创建交易
 */
fun RawTransaction.Companion.optionTransaction(
    senderAddress: String,
    payload: TransactionPayload,
    sequenceNumber: Long,
    maxGasAmount: Long = 280_000,
    gasUnitPrice: Long = 0,
    delayed: Long = 1000
): RawTransaction {
    val rawTransaction = RawTransaction(
        AccountAddress(HexUtils.fromHex(senderAddress)),
        sequenceNumber,
        payload,
        maxGasAmount,
        gasUnitPrice,
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
    amount: Long
): TransactionPayload {
//    val moveEncode = Move.decode(context.assets.open("move/libra_peer_to_peer_transfer.json"))
    val moveEncode =
        "a11ceb0b010007014600000004000000034a000000060000000c50000000060000000d5600000006000000055c0000002900000004850000002000000007a50000000f00000000000002000100010300020002050300030205030300063c53454c463e046d61696e0c4c696272614163636f756e740f7061795f66726f6d5f73656e6465720000000000000000000000000000000000000000000000000000000000000000000000020004000b000b0112010102".hexToBytes()

    val addressArgument = TransactionArgument.newAddress(address)
    val amountArgument = TransactionArgument.newU64(amount)

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf(addressArgument, amountArgument)
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

//    val moveEncode = Move.decode(
//        context.assets.open("move/libra_transfer_with_data.json")
//    )
    val moveEncode =
        "a11ceb0b010007014600000004000000034a000000060000000c50000000080000000d580000000800000005600000003700000004970000002000000007b7000000110000000000000200010001030002000305030b0200030305030b020300063c53454c463e046d61696e0c4c696272614163636f756e741d7061795f66726f6d5f73656e6465725f776974685f6d657461646174610000000000000000000000000000000000000000000000000000000000000000000000030005000b000b010b0212010102".hexToBytes()

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf(addressArgument, amountArgument, dateArgument)
        )
    )
}