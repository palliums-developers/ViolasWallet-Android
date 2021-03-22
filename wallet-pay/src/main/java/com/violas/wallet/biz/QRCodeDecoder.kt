package com.violas.wallet.biz

import android.os.Parcelable
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.common.*
import com.violas.wallet.repository.DataRepository
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.palliums.libracore.wallet.AccountIdentifier
import org.palliums.violascore.wallet.IntentIdentifier
import java.lang.RuntimeException
import java.util.*

class DiemChainNetworkDifferentException : RuntimeException(
    getString(
        R.string.common_tips_chain_network_is_different_format,
        "Diem"
    )
)

class ViolasChainNetworkDifferentException : RuntimeException(
    getString(
        R.string.common_tips_chain_network_is_different_format,
        "Violas"
    )
)

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
    val coinNumber: Int,
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

    val coinType = when (transferQRCodeBean.chainName?.toLowerCase(Locale.CHINA)) {
        getBitcoinCoinType().chainName().toLowerCase(Locale.CHINA) -> {
            getBitcoinCoinType()
        }
        getDiemCoinType().chainName().toLowerCase(Locale.CHINA) -> {
            if (transferQRCodeBean.chainId != getDiemChainId()) {
                throw DiemChainNetworkDifferentException()
            }
            getDiemCoinType()
        }
        getViolasCoinType().chainName().toLowerCase(Locale.CHINA) -> {
            if (transferQRCodeBean.chainId != getViolasChainId()) {
                throw ViolasChainNetworkDifferentException()
            }
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
    val chainName: String?,
    val chainId: Int?,
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
        diemNetworkPrefixToChainId(intentIdentifier.getAccountIdentifier().getPrefix()),
        intentIdentifier.getAccountIdentifier().getAccountAddress().toHex(),
        intentIdentifier.getAccountIdentifier().getSubAddress().toHex(),
        intentIdentifier.getAmount(),
        "",
        intentIdentifier.getCurrency()
    )
}

private fun diemNetworkPrefixToChainId(
    networkPrefix: AccountIdentifier.NetworkPrefix
): Int? {
    return when (networkPrefix) {
        AccountIdentifier.NetworkPrefix.MainnetPrefix -> 1
        AccountIdentifier.NetworkPrefix.TestnetPrefix -> 2
        AccountIdentifier.NetworkPrefix.PreMainnetPrefix -> 5
        else -> null
    }
}

private fun decodeViolasTransferQRCode(content: String): TransferQRCodeBean {
    val intentIdentifier = IntentIdentifier.decode(content)
    return TransferQRCodeBean(
        "violas",
        violasNetworkPrefixToChainId(intentIdentifier.getAccountIdentifier().getPrefix()),
        intentIdentifier.getAccountIdentifier().getAccountAddress().toHex(),
        intentIdentifier.getAccountIdentifier().getSubAddress().toHex(),
        intentIdentifier.getAmount(),
        null,
        intentIdentifier.getCurrency()
    )
}

private fun violasNetworkPrefixToChainId(
    networkPrefix: org.palliums.violascore.wallet.AccountIdentifier.NetworkPrefix
): Int? {
    return when (networkPrefix) {
        org.palliums.violascore.wallet.AccountIdentifier.NetworkPrefix.MainnetPrefix -> 1
        org.palliums.violascore.wallet.AccountIdentifier.NetworkPrefix.TestnetPrefix -> 2
        org.palliums.violascore.wallet.AccountIdentifier.NetworkPrefix.PreMainnetPrefix -> 5
        else -> null
    }
}

private fun decodeTransferQRCode(content: String): TransferQRCodeBean {
    if (!content.contains(":") || content.contains("://")) {
        return TransferQRCodeBean(null, null, content)
    }
    var tokenName: String? = null
    val chainNames = content.split(":")
    val chainName = if (chainNames[0].isEmpty()) {
        null
    } else {
        chainNames[0].toLowerCase(Locale.CHINA)
    }
    val addresses = chainNames[1].split("?")
    var amount = 0L
    var label: String? = null
    if (addresses.size > 1) {
        addresses[1].split("&")
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
    return TransferQRCodeBean(chainName, 2, addresses[0], null, amount, label)
}

private fun decodeWalletConnectQRCode(content: String): Boolean {
    val regex = Regex("wc:\\S+bridge=\\S+key=\\S+")
    return regex.matches(content)
}
