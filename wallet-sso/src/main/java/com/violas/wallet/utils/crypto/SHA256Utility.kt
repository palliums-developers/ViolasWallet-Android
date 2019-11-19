package com.palliums.pay.util.crypto

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object SHA256Utility {

    @JvmOverloads
    fun sha256(bytes: ByteArray, offset: Int = 0, size: Int = bytes.size): ByteArray {
        val sha256Digest = MessageDigest.getInstance("SHA-256")
        sha256Digest.update(bytes, offset, size)
        return sha256Digest.digest()
    }

    @JvmOverloads
    fun doubleSha256(bytes: ByteArray, offset: Int = 0, size: Int = bytes.size): ByteArray? {
        try {
            val sha256 = MessageDigest.getInstance("SHA-256")
            sha256.update(bytes, offset, size)
            return sha256.digest(sha256.digest())
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return null
    }
}