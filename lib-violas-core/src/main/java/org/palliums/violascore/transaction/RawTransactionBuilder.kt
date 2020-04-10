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
    maxGasAmount: Long = 400_000,
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
    amount: Long
): TransactionPayload {
    val moveEncode = Move.decode(context.assets.open("move/peer_to_peer_transfer.json"))

    val addressArgument = TransactionArgument.newAddress(address)
    val amountArgument = TransactionArgument.newU64(amount)

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf(addressArgument, amountArgument)
        )
    )
}

/**
 * 创建 Token 转账 payload
 */
fun TransactionPayload.Companion.optionTokenTransactionPayload(
    context: Context,
    tokenAddress: String,
    address: String,
    amount: Long
): TransactionPayload {
    val moveEncode = Move.violasTokenEncode(
        context.assets.open("move/token_transfer.json"),
        tokenAddress.hexToBytes()
    )

    val addressArgument = TransactionArgument.newAddress(address)
    val amountArgument = TransactionArgument.newU64(amount)

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf(addressArgument, amountArgument)
        )
    )
}

/**
 * 注册 Token 的 payload
 */
fun TransactionPayload.Companion.optionPublishTokenPayload(
    context: Context,
    tokenAddress: String
): TransactionPayload {
    val moveEncode = Move.violasTokenEncode(
        context.assets.open("move/token_publish.json"),
        tokenAddress.hexToBytes()
    )

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf()
        )
    )
}

/**
 * 创建发起兑换交易 Payload
 */
fun TransactionPayload.Companion.optionExchangePayload(
    context: Context,
    receiveAddress: String,
    sendTokenAddress: String,
    exchangeTokenAddress: String,
    exchangeSendAmount: Long,
    exchangeReceiveAmount: Long
): TransactionPayload {

    val subExchangeDate = JSONObject()
    subExchangeDate.put("type", "sub_ex")
    subExchangeDate.put("addr", exchangeTokenAddress)
    subExchangeDate.put("amount", exchangeReceiveAmount)
    subExchangeDate.put("fee", 0)
    subExchangeDate.put("exp", 1000)

    val addressArgument = TransactionArgument.newAddress(receiveAddress)
    val amountArgument = TransactionArgument.newU64(exchangeSendAmount)
    val dateArgument =
        TransactionArgument.newByteArray(subExchangeDate.toString().toByteArray())

    val moveEncode = Move.violasTokenEncode(
        context.assets.open("move/violas_token_transfer_with_data.json"),
        sendTokenAddress.hexToBytes()
    )

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf(addressArgument, amountArgument, dateArgument)
        )
    )
}

/**
 * 创建撤销兑换交易 Payload
 */
fun TransactionPayload.Companion.optionUndoExchangePayload(
    context: Context,
    receiveAddress: String,
    sendTokenAddress: String,
    version: Long
): TransactionPayload {
    val subExchangeDate = JSONObject()
    subExchangeDate.put("type", "wd_ex")
    subExchangeDate.put("ver", version)

    val addressArgument = TransactionArgument.newAddress(receiveAddress)
    val amountArgument = TransactionArgument.newU64(0)
    val dateArgument =
        TransactionArgument.newByteArray(subExchangeDate.toString().toByteArray())

    val moveEncode = Move.violasTokenEncode(
        context.assets.open("move/violas_token_transfer_with_data.json"),
        sendTokenAddress.hexToBytes()
    )

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf(addressArgument, amountArgument, dateArgument)
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
        context.assets.open("move/violas_transfer_with_data.json")
    )

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf(addressArgument, amountArgument, dateArgument)
        )
    )
}

fun TransactionPayload.Companion.optionTokenWithDataPayload(
    context: Context,
    receiveAddress: String,
    exchangeSendAmount: Long,
    tokenAddress: String,
    data: ByteArray
): TransactionPayload {

    val addressArgument = TransactionArgument.newAddress(receiveAddress)
    val amountArgument = TransactionArgument.newU64(exchangeSendAmount)
    val dateArgument = TransactionArgument.newByteArray(data)

    val moveEncode = Move.violasTokenEncode(
        context.assets.open("move/violas_token_transfer_with_data.json"),
        tokenAddress.hexToBytes()
    )

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf(addressArgument, amountArgument, dateArgument)
        )
    )
}

/**
 * 生成链上注册稳定比交易 Payload
 *
 * @param tokenAddress 稳定币的 model address
 */
fun TransactionPayload.Companion.optionTokenRegisterPayload(
    context: Context,
    tokenAddress: String
): TransactionPayload {

    val moveEncode = Move.violasTokenMultiEncode(
        context.assets.open("move/violas_token.json"),
        tokenAddress.hexToBytes()
    )

    return TransactionPayload(
        TransactionPayload.Module(
            moveEncode
        )
    )
}

/**
 * 生成铸币交易 Payload
 *
 * @param tokenAddress 稳定币的 model address
 * @param receiveAddress 稳定币接收地址
 * @param receiveAmount 铸币数量(最小单位)
 */
fun TransactionPayload.Companion.optionTokenMintPayload(
    context: Context,
    tokenAddress: String,
    receiveAddress: String,
    receiveAmount: Long
): TransactionPayload {

    val addressArgument = TransactionArgument.newAddress(receiveAddress)
    val amountArgument = TransactionArgument.newU64(receiveAmount)

    val moveEncode = Move.violasTokenEncode(
        context.assets.open("move/violas_token_mint.json"),
        tokenAddress.hexToBytes()
    )

    return TransactionPayload(
        TransactionPayload.Script(
            moveEncode,
            arrayListOf(addressArgument, amountArgument)
        )
    )
}