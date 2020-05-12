package org.palliums.violascore.move

import org.json.JSONObject
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.utils.sundaySearch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader

object Move {
    val defaultTokenAddress by lazy {
        "7257c2417e4d1038e1817c8f283ace2e".hexToBytes()
    }

    fun decode(inputStream: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()
        buffer.use { output ->
            inputStream.use { input ->
                var nRead: Int = -1
                val data = ByteArray(16384)

                while (input.read(data, 0, data.size).also { nRead = it } != -1) {
                    output.write(data, 0, nRead)
                }
            }
            return buffer.toByteArray()
        }
    }

    fun decodeJson(moveJson: String): ByteArray {
        val jsonObject = JSONObject(moveJson)
        if (jsonObject.has("code")) {
            val jsonArray = jsonObject.getJSONArray("code")
            val moveCode = ByteArray(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                moveCode[i] = jsonArray.getInt(i).toByte()
            }
            return moveCode
        }
        return ByteArray(0)
    }

    fun violasTokenEncode(
        inputStream: InputStream,
        token: ByteArray
    ): ByteArray {
        val decode = decode(inputStream)
        val findAddressIndex = findAddressIndex(decode, defaultTokenAddress)
        if (findAddressIndex != -1) {
            System.arraycopy(token, 0, decode, findAddressIndex, token.size)
        }
        return decode
    }

    fun violasTokenMultiEncode(
        inputStream: InputStream,
        token: ByteArray
    ): ByteArray {
        val decode = decode(inputStream)
        var exists = true
        do {
            val findAddressIndex = findAddressIndex(decode, defaultTokenAddress)
            if (findAddressIndex != -1) {
                System.arraycopy(token, 0, decode, findAddressIndex, token.size)
            } else {
                exists = false
            }
        } while (exists)
        return decode
    }

    fun violasReplaceAddress(
        mvCode: ByteArray,
        tokenAddress: ByteArray,
        replaceTokenAddress: ByteArray = defaultTokenAddress
    ): ByteArray {
        val findAddressIndex = findAddressIndex(mvCode, replaceTokenAddress)
        if (findAddressIndex != -1) {
            System.arraycopy(tokenAddress, 0, mvCode, findAddressIndex, tokenAddress.size)
        }
        return mvCode
    }

    fun violasMultiReplaceAddress(mvCode: ByteArray, tokenAddress: ByteArray): ByteArray {
        var exists = true
        do {
            val findAddressIndex = findAddressIndex(mvCode, defaultTokenAddress)
            if (findAddressIndex != -1) {
                System.arraycopy(tokenAddress, 0, mvCode, findAddressIndex, tokenAddress.size)
            } else {
                exists = false
            }
        } while (exists)
        return mvCode
    }

    fun findAddressIndex(moveCode: ByteArray, tokenAddress: ByteArray): Int {
        return sundaySearch(moveCode, tokenAddress)
    }
}