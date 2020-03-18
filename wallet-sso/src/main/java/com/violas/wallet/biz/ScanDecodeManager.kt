package com.violas.wallet.biz

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.SCAN_ACTION_LOGIN_DESKTOP
import com.violas.wallet.common.Vm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

enum class ScanCodeType {
    Address, Text, LoginDesktop
}

open class ScanBean(
    val msg: String
)

class ScanTranBean(
    msg: String,
    val coinType: Int,
    val address: String,
    var amount: Long = 0,
    var label: String? = null,
    val tokenName: String? = null
) : ScanBean(msg)

/**
 * 解析扫码内容
 * @param callback 回掉
 * @param msg 消息
 */
fun decodeScanQRCode(
    msg: String,
    callback: (scanType: ScanCodeType, msg: ScanBean) -> Unit
) {
    GlobalScope.launch(Dispatchers.IO) {
        val scanLoginData = decodeScanLoginData(msg)
        if (!scanLoginData.isNullOrEmpty()) {
            callback.invoke(ScanCodeType.LoginDesktop, ScanBean(scanLoginData))
            return@launch
        }

        val splitMsg = splitMsg(msg)
        var scanType = ScanCodeType.Address
        val coinType = when (splitMsg.coinType?.toLowerCase(Locale.CHINA)) {
            CoinTypes.Bitcoin.coinName().toLowerCase(Locale.CHINA) -> {
                if (Vm.TestNet) {
                    CoinTypes.BitcoinTest.coinType()
                } else {
                    CoinTypes.Bitcoin.coinType()
                }
            }
            CoinTypes.Libra.coinName().toLowerCase(Locale.CHINA) -> {
                CoinTypes.Libra.coinType()
            }
            CoinTypes.Violas.coinName().toLowerCase(Locale.CHINA) -> {
                CoinTypes.Violas.coinType()
            }
            else -> {
                scanType = ScanCodeType.Text
                -100
            }
        }
        when (scanType) {
            ScanCodeType.Text -> {
                callback.invoke(
                    scanType,
                    ScanBean(msg)
                )
            }
            else -> {
                callback.invoke(
                    scanType,
                    ScanTranBean(
                        msg,
                        coinType,
                        splitMsg.address,
                        splitMsg.amount,
                        splitMsg.label,
                        splitMsg.tokenName
                    )
                )
            }
        }
    }
}

private data class QRCodeMsg(
    val coinType: String?,
    val address: String,
    var amount: Long = 0,
    var label: String? = null,
    val tokenName: String? = null
)

private fun splitMsg(msg: String): QRCodeMsg {
    if (!msg.contains(":")) {
        return QRCodeMsg(null, msg)
    }
    var tokenName: String? = null
    val coinTypeSplit = msg.split(":")
    val coinType = if (coinTypeSplit[0].isEmpty()) {
        null
    } else {
        val prefix = coinTypeSplit[0].toLowerCase(Locale.CHINA)
        if (prefix.indexOf("-") != -1) {
            val split = prefix.split("-")
            tokenName = split[1]
            split[0]
        } else {
            prefix
        }
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
    return QRCodeMsg(coinType, addressSplit[0], amount, label, tokenName)
}

private data class ScanLoginBean(
    @SerializedName("type")
    val type: Int,
    @SerializedName("session_id")
    val sessionId: String
)

private fun decodeScanLoginData(content: String): String? {
    return try {
        val bean = Gson().fromJson(content, ScanLoginBean::class.java)
        if (bean.type == SCAN_ACTION_LOGIN_DESKTOP && bean.sessionId.isNotEmpty())
            bean.sessionId
        else
            null
    } catch (ignore: Exception) {
        null
    }
}