package com.smallraw.core.crypto

import android.annotation.SuppressLint
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class AESDigest {
    companion object {
        fun encrypt(content: ByteArray, password: ByteArray): ByteArray {
            return operation(Cipher.ENCRYPT_MODE, content, password)
        }

        fun decrypt(content: ByteArray, password: ByteArray): ByteArray? {
            try {
                return operation(Cipher.DECRYPT_MODE, content, password)
            } catch (e: BadPaddingException) {
                return null
            }
        }

        fun generaKey(seed: ByteArray): ByteArray {
//            val keyGenerator = KeyGenerator.getInstance("AES")
//            val secureRandom = SecureRandom.getInstance("SHA1PRNG")
//            secureRandom.setSeed(seed)
//            keyGenerator.init(256, secureRandom) //256 bits or 128 bits,192bits
//            return keyGenerator.generateKey().encoded
            return SecretKeySpec(seed,"AES").encoded
        }

        @SuppressLint("GetInstance")
        private fun operation(mode: Int, content: ByteArray, password: ByteArray): ByteArray {
            val key = SecretKeySpec(generaKey(password), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(mode, key, IvParameterSpec(ByteArray(cipher.blockSize)))
            return cipher.doFinal(content)
        }
    }

}
