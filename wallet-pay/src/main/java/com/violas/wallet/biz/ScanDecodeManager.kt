package com.violas.wallet.biz

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.Vm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

enum class ScanCodeType {
    Address, Text, Login, WalletConnectSocket
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

class ScanLoginBean(
    msg: String,
    val loginType: Int,
    val sessionId: String
) : ScanBean(msg)

/**
 * 解析扫码内容
 * @param callback 回掉
 * @param msg 二维码
 */
fun decodeScanQRCode(
    msg: String,
    callback: (scanType: ScanCodeType, content: ScanBean) -> Unit
) {
    GlobalScope.launch(Dispatchers.IO) {
        val loginQRCode = decodeLoginQRCode(msg)
        if (loginQRCode != null) {
            callback.invoke(
                ScanCodeType.Login,
                ScanLoginBean(msg, loginQRCode.type, loginQRCode.sessionId)
            )
            return@launch
        }

        if (decodeWalletConnectSocketQRCode(msg)) {
            callback.invoke(
                ScanCodeType.WalletConnectSocket,
                ScanBean(msg)
            )
            return@launch
        }

        val tranQRCode = decodeTranQRCode(msg)
        var scanType = ScanCodeType.Address
        val coinType = when (tranQRCode.coinType?.toLowerCase(Locale.CHINA)) {
            CoinTypes.Bitcoin.fullName().toLowerCase(Locale.CHINA) -> {
                if (Vm.TestNet) {
                    CoinTypes.BitcoinTest
                } else {
                    CoinTypes.Bitcoin
                }
            }
            CoinTypes.Libra.fullName().toLowerCase(Locale.CHINA) -> {
                CoinTypes.Libra
            }
            CoinTypes.Violas.fullName().toLowerCase(Locale.CHINA) -> {
                CoinTypes.Violas
            }
            else -> {
                scanType = ScanCodeType.Text
                null
            }
        }
        when {
            coinType == null ||
                    scanType == ScanCodeType.Text -> {
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
                        coinType.coinType(),
                        tranQRCode.address,
                        tranQRCode.amount,
                        tranQRCode.label,
                        tranQRCode.tokenName
                    )
                )
            }
        }
    }
}

private data class TranQRCode(
    val coinType: String?,
    val address: String,
    var amount: Long = 0,
    var label: String? = null,
    val tokenName: String? = null
)

private fun decodeTranQRCode(msg: String): TranQRCode {
    if (!msg.contains(":")) {
        return TranQRCode(null, msg)
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
    return TranQRCode(coinType, addressSplit[0], amount, label, tokenName)
}

private data class LoginQRCode(
    @SerializedName("type")
    val type: Int,
    @SerializedName("session_id")
    val sessionId: String
)

private fun decodeLoginQRCode(content: String): LoginQRCode? {
    return try {
        val bean = Gson().fromJson(content, LoginQRCode::class.java)
        if (bean.type > 0 && bean.sessionId.isNotEmpty())
            bean
        else
            null
    } catch (ignore: Exception) {
        null
    }
}

private fun decodeWalletConnectSocketQRCode(msg: String): Boolean {
    val regex = Regex("wc:\\S+bridge=\\S+key=\\S+")
    return regex.matches(msg)
}
