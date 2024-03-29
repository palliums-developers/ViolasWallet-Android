package org.palliums.violascore.move

import org.json.JSONObject
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.utils.sundaySearch
import java.io.InputStream
import java.io.InputStreamReader

object Move {
    val defaultTokenAddress by lazy {
        "7257c2417e4d1038e1817c8f283ace2e1041b3396cdbb099eb357bbee024d614".hexToBytes()
    }

    fun decode(inputStream: InputStream): ByteArray {
        InputStreamReader(inputStream).use {
            val moveJson = it.readText()
            return decode(moveJson)
        }
    }

    fun decode(moveJson: String): ByteArray {
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

    fun findAddressIndex(moveCode: ByteArray, tokenAddress: ByteArray): Int {
        return sundaySearch(moveCode, tokenAddress)
    }
}