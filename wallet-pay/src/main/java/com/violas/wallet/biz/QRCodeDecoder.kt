package com.violas.wallet.biz

import android.os.Parcelable
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
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
 * 解析二维码
 * @param content 二维码内容
 */
suspend fun decodeQRCode(content: String): QRCode = withContext(Dispatchers.IO) {
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
        getBitcoinCoinType().chainName().toLowerCase(Locale.CHINA) -> {
            getBitcoinCoinType()
        }
        getDiemCoinType().chainName().toLowerCase(Locale.CHINA) -> {
            getDiemCoinType()
        }
        getViolasCoinType().chainName().toLowerCase(Locale.CHINA) -> {
            getViolasCoinType()
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
                .findAllByCoinType(coinType.coinNumber())?.get(0)
            account?.let {
                val token = DataRepository.getTokenStorage()
                    .findByModelName(account.id, transferQRCodeBean.tokenName)
                tokenName = token?.assetsName
            }
        }
        TransferQRCode(
            content,
            coinType.coinNumber(),
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

private fun decodeWalletConnectQRCode(content: String): Boolean {
    val regex = Regex("wc:\\S+bridge=\\S+key=\\S+")
    return regex.matches(content)
}
