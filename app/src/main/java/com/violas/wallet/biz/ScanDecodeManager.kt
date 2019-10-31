package com.violas.wallet.biz

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.Vm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * 解析扫码内容
 * @param callback 回掉
 * @param msg 消息
 */
fun decodeScanQRCode(
    msg: String,
    callback: (coinType: Int, address: String, amount: Long) -> Unit
) {
    GlobalScope.launch(Dispatchers.IO) {

        val splitMsg = splitMsg(msg)
        val coinType = when (splitMsg.coinType?.toLowerCase(Locale.CHINA)) {
            CoinTypes.Bitcoin.fullName() -> {
                if (Vm.TestNet) {
                    CoinTypes.BitcoinTest.coinType()
                } else {
                    CoinTypes.Bitcoin.coinType()
                }
            }
            CoinTypes.Libra.fullName() -> {
                CoinTypes.Libra.coinType()
            }
            CoinTypes.VToken.fullName() -> {
                CoinTypes.VToken.coinType()
            }
            else -> {
                -1
            }
        }
        callback.invoke(coinType, splitMsg.address, splitMsg.amount)
    }
}

data class QRCodeMsg(
    val coinType: String?,
    val address: String,
    var amount: Long = 0,
    var label: String? = null
)

fun splitMsg(msg: String): QRCodeMsg {
    if (!msg.contains(":")) {
        return QRCodeMsg(null, msg)
    }
    val coinTypeSplit = msg.split(":")
    val coinType = if (coinTypeSplit[0].isEmpty()) {
        null
    } else {
        coinTypeSplit[0].toLowerCase(Locale.CHINA)
    }
    val addressSplit = coinTypeSplit[1].split("?")
    var amount = 0L
    var label: String? = null
    if (addressSplit.size > 1) {
        addressSplit[1].split("&")
            .forEach {
                val paramSplit = it.split("=")
                when (paramSplit[0].toLowerCase(Locale.CHINA)) {
                    "amount" -> {
                        try {
                            amount = paramSplit[1].toLong()
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                    }
                    "label" -> {
                        label = paramSplit[1]
                    }
                }
            }
    }
    return QRCodeMsg(coinType, addressSplit[0], amount, label)
}