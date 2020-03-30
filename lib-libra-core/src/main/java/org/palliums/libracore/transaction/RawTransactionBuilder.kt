package org.palliums.libracore.transaction

import android.content.Context
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.transaction.storage.StructTag
import org.palliums.libracore.transaction.storage.TypeTag
import org.palliums.libracore.utils.HexUtils
import org.spongycastle.asn1.x500.style.RFC4519Style.name
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
        lbr_struct_tag(),
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
    authenticationKey: String,
    amount: Long
): TransactionPayload {
//    val moveEncode = Move.decode(context.assets.open("move/libra_peer_to_peer_transfer.json"))
    val moveEncode =
//        "a11ceb0b010007014600000004000000034a000000060000000c50000000060000000d5600000006000000055c0000002900000004850000002000000007a50000000f00000000000002000100010300020002050300030205030300063c53454c463e046d61696e0c4c696272614163636f756e740f7061795f66726f6d5f73656e6465720000000000000000000000000000000000000000000000000000000000000000000000020004000b000b0112010102".hexToBytes()
        "a11ceb0b010008014f000000060000000255000000040000000359000000060000000c5f000000110000000d700000000b000000057b0000002f00000004aa0000001000000007ba0000001300000000000001000201030200020400000501020003050a02030101020003050a0203000303050a02030301080000063c53454c463e034c42520c4c696272614163636f756e7401540f7061795f66726f6d5f73656e646572046d61696e00000000000000000000000000000000010000ffff030005000a000b010a0212000102".hexToBytes()
    val address = authenticationKey.substring(authenticationKey.length / 2)
    val authenticationKeyPrefix = authenticationKey.substring(0, authenticationKey.length / 2)
    val addressArgument = TransactionArgument.newAddress(address)
    val authenticationKeyPrefixArgument =
        TransactionArgument.newByteArray(authenticationKeyPrefix.hexToBytes())
    val amountArgument = TransactionArgument.newU64(amount)

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf(addressArgument, authenticationKeyPrefixArgument, amountArgument)
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


private fun lbr_struct_tag(): TypeTag {
    return TypeTag.newStructTag(
        StructTag(
            AccountAddress.DEFAULT,
            "LBR",
            "T",
            arrayListOf()
        )
    )
}