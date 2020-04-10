package org.palliums.libracore.move

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream


object Move {
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

    fun violasTokenEncode(move: ByteArray, token: ByteArray): ByteArray {
        System.arraycopy(token, 0, move, 107, token.size)
        return move
    }
}