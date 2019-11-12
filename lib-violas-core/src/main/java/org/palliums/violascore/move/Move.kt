package org.palliums.violascore.move

import org.json.JSONObject
import java.io.InputStream
import java.io.InputStreamReader

object Move {
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

    fun violasTokenEncode(move: ByteArray, token: ByteArray): ByteArray {
        System.arraycopy(token, 0, move, 107, token.size)
        return move
    }
}