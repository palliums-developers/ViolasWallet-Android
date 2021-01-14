package com.violas.wallet.biz

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.palliums.violascore.wallet.IntentIdentifier
import java.util.*

/**
 * 二维码
 */
interface QRCode : Parcelable

/**
 * 普通二维码
 */
@Parcelize
class CommonQRCode(
    val content: String
) : QRCode

/**
 * WalletConnect二维码
 */
@Parcelize
class WalletConnectQRCode(
    val content: String
) : QRCode

/**
 * 转账二维码
 */
@Parcelize
class TransferQRCode(
    val content: String,
    val coinType: Int,
    val address: String,
    val subAddress: String? = null,
    var amount: Long = 0,
    var label: String? = null,
    val tokenName: String? = null
) : QRCode

/**
 * 登录二维码
 */
@Parcelize
class LoginQRCode(
    val content: String,
    val loginType: Int,
    val sessionId: String
) : QRCode

/**
 * 解析二维码
 * @param content 二维码内容
 */
suspend fun decodeQRCode(content: String): QRCode = withContext(Dispatchers.IO) {
    val loginQRCodeBean = decodeLoginQRCode(content)
    if (loginQRCodeBean != null) {
        return@withContext LoginQRCode(content, loginQRCodeBean.type, loginQRCodeBean.sessionId)
    }

    if (decodeWalletConnectQRCode(content)) {
        return@withContext WalletConnectQRCode(content)
    }

    val transferQRCodeBean = try {
        try {
            decodeViolasTransferQRCode(content)
        } catch (e: Exception) {
            decodeLibraTransferQRCode(content)
        }
    } catch (e: Exception) {
        decodeTransferQRCode(content)
    }

    val coinType = when (transferQRCodeBean.coinType?.toLowerCase(Locale.CHINA)) {
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
            null
        }
    }

    return@withContext if (coinType == null) {
        CommonQRCode(content)
    } else {
        var tokenName: String? = null
        transferQRCodeBean.tokenName?.let {
            val account = DataRepository.getAccountStorage()
                .findAllByCoinType(coinType.coinType())?.get(0)
            account?.let {
                val token = DataRepository.getTokenStorage()
                    .findByModelName(account.id, transferQRCodeBean.tokenName)
                tokenName = token?.assetsName
            }
        }
        TransferQRCode(
            content,
            coinType.coinType(),
            transferQRCodeBean.address,
            transferQRCodeBean.subAddress,
            transferQRCodeBean.amount,
            transferQRCodeBean.label,
            tokenName
        )
    }
}

private data class TransferQRCodeBean(
    val coinType: String?,
    val address: String,
    val subAddress: String? = null,
    var amount: Long = 0,
    var label: String? = null,
    val tokenName: String? = null
)

private fun decodeLibraTransferQRCode(content: String): TransferQRCodeBean {
    val intentIdentifier = org.palliums.libracore.wallet.IntentIdentifier.decode(content)
    return TransferQRCodeBean(
        "libra",
        intentIdentifier.getAccountIdentifier().getAccountAddress().toHex(),
        intentIdentifier.getAccountIdentifier().getSubAddress().toHex(),
        intentIdentifier.getAmount(),
        "",
        intentIdentifier.getCurrency()
    )
}

private fun decodeViolasTransferQRCode(content: String): TransferQRCodeBean {
    val intentIdentifier = IntentIdentifier.decode(content)
    return TransferQRCodeBean(
        "violas",
        intentIdentifier.getAccountIdentifier().getAccountAddress().toHex(),
        intentIdentifier.getAccountIdentifier().getSubAddress().toHex(),
        intentIdentifier.getAmount(),
        null,
        intentIdentifier.getCurrency()
    )
}

private fun decodeTransferQRCode(content: String): TransferQRCodeBean {
    if (!content.contains(":") || content.contains("://")) {
        return TransferQRCodeBean(null, content)
    }
    var tokenName: String? = null
    val coinTypeSplit = content.split(":")
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
    return TransferQRCodeBean(coinType, addressSplit[0], null, amount, label)
}

private data class LoginQRCodeBean(
    @SerializedName("type")
    val type: Int,
    @SerializedName("session_id")
    val sessionId: String
)

private fun decodeLoginQRCode(content: String): LoginQRCodeBean? {
    return try {
        val bean = Gson().fromJson(content, LoginQRCodeBean::class.java)
        if (bean.type > 0 && bean.sessionId.isNotEmpty())
            bean
        else
            null
    } catch (ignore: Exception) {
        null
    }
}

private fun decodeWalletConnectQRCode(content: String): Boolean {
    val regex = Regex("wc:\\S+bridge=\\S+key=\\S+")
    return regex.matches(content)
}
