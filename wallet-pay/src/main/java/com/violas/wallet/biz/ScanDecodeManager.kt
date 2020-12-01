package com.violas.wallet.biz

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.palliums.utils.coroutineExceptionHandler
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.palliums.violascore.wallet.IntentIdentifier
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
    GlobalScope.launch(Dispatchers.IO + coroutineExceptionHandler()) {
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

        val tranQRCode = try {
            try {
                decodeViolasQRCode(msg)
            } catch (e: Exception) {
                decodeLibraQRCode(msg)
            }
        } catch (e: Exception) {
            decodeTranQRCode(msg)
        }

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
                var name: String? = null
                tranQRCode.tokenName?.let {
                    val account =
                        DataRepository.getAccountStorage().findAllByCoinType(coinType.coinType())
                            ?.get(0)
                    account?.let {
                        val token = DataRepository.getTokenStorage()
                            .findByModelName(account.id, tranQRCode.tokenName)
                        name = token?.assetsName
                    }
                }

                callback.invoke(
                    scanType,
                    ScanTranBean(
                        msg,
                        coinType.coinType(),
                        tranQRCode.address,
                        tranQRCode.amount,
                        tranQRCode.label,
                        name
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

private fun decodeLibraQRCode(msg: String): TranQRCode {
    val decode = org.palliums.libracore.wallet.IntentIdentifier.decode(msg)

    return TranQRCode(
        "libra",
        decode.getAccountIdentifier().getAccountAddress().toHex(),
        decode.getAmount(),
        "",
        decode.getCurrency()
    )
}

private fun decodeViolasQRCode(msg: String): TranQRCode {
    val decode = IntentIdentifier.decode(msg)

    return TranQRCode(
        "violas",
        decode.getAccountIdentifier().getAccountAddress().toHex(),
        decode.getAmount(),
        null,
        decode.getCurrency()
    )
}

private fun decodeTranQRCode(msg: String): TranQRCode {
    if (!msg.contains(":") || msg.contains("://")) {
        return TranQRCode(null, msg)
    }
    var tokenName: String? = null
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
    return TranQRCode(coinType, addressSplit[0], amount, label)
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
