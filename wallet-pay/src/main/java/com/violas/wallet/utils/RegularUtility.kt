package com.violas.wallet.utils

import com.quincysx.crypto.utils.SHA256
import com.smallraw.core.crypto.Base58Utility
import com.smallraw.core.crypto.Bech32Utility
import org.palliums.libracore.serialization.hexToBytes
import java.util.*

fun validationChinaPhone(phoneNumber: String): Boolean {
    if (phoneNumber.isEmpty()) {
        return false
    }
    val regex =
        Regex("[1](([3][0-9])|([4][5-9])|([5][0-3,5-9])|([6][5,6])|([7][0-8])|([8][0-9])|([9][189]))[0-9]{8}")
    return regex.matches(phoneNumber)
}

fun validationHkPhone(phoneNumber: String): Boolean {
    if (phoneNumber.isEmpty()) {
        return false
    }
    val regex = """[5689][0-9]{7}""".toRegex()

    return regex.matches(phoneNumber)
}

fun validationEmailAddress(emailAddress: String): Boolean {
    if (emailAddress.isEmpty()) {
        return false
    }
    val regex =
        """([A-Za-z0-9_\-\.\u4e00-\u9fa5])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,8})""".toRegex()
    return regex.matches(emailAddress)
}

fun validationBTCAddress(address: String): Boolean {
    return validationBTCNormalAddress(address) || validationBTCBech32Address(address)
}

fun validationLibraAddress(address: String): Boolean {
    try {
        address.hexToBytes()
    } catch (e: Exception) {
        return false
    }
    if (address.length == 32 || address.length == 64) {
        return true
    }
    return false
}

fun validationViolasAddress(address: String): Boolean {
    try {
        address.hexToBytes()
    } catch (e: Exception) {
        return false
    }
    if (address.length == 64) {
        return true
    }
    return false
}

private fun validationBTCBech32Address(address: String): Boolean {
    try {
        val decode = Bech32Utility.decode(address)
        var isCheck: Boolean

        when (decode.hrp) {
            "bc",
            "tb",
            "bcrt" -> {
                isCheck = true
            }
            else -> {
                isCheck = false
            }
        }
        return isCheck
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

val TEST_NET_ADDRESS_SUFFIX = 0x6f.toByte()
val MAIN_NET_ADDRESS_SUFFIX = 0x00.toByte()
val TEST_P2SH_ADDRESS_PREFIX = 0xC4.toByte()
val MAIN_P2SH_ADDRESS_PREFIX = 0x5.toByte()
private fun validationBTCNormalAddress(address: String): Boolean {
    try {
        val decode = Base58Utility.decode(address)

        var isCheck: Boolean

        when (decode[0]) {
            TEST_NET_ADDRESS_SUFFIX,
            TEST_P2SH_ADDRESS_PREFIX,
            MAIN_NET_ADDRESS_SUFFIX,
            MAIN_P2SH_ADDRESS_PREFIX -> {
                isCheck = true
            }
            else -> {
                isCheck = false
            }
        }

        val oldCheck = ByteArray(4)
        System.arraycopy(decode, decode.size - 4, oldCheck, 0, oldCheck.size)

        val newCheck = ByteArray(4)
        val sha256 = SHA256.doubleSha256(decode, 0, decode.size - 4)
        System.arraycopy(sha256, 0, newCheck, 0, newCheck.size)

        for (i in 0 until 4) {
            if (newCheck[i] != oldCheck[i]) {
                isCheck = false
                break
            }
        }

        Arrays.fill(decode, 0.toByte())
        Arrays.fill(oldCheck, 0.toByte())
        Arrays.fill(newCheck, 0.toByte())
        Arrays.fill(sha256, 0.toByte())
        return isCheck
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}