package com.smallraw.core.crypto

import kotlin.math.abs

open class SecurityEngine(
    private val mStorageUniqueKey: StorageUniqueKey? = null
) {
    companion object {
        private val salt = "Random_SecurityEngine".toByteArray()
    }

    private fun obtainUniqueKey(): ByteArray {
        val uniqueKey = mStorageUniqueKey?.obtainUniqueKey() ?: ByteArray(0)
        if (uniqueKey.isEmpty()) {
            return uniqueKey
        }
        val obtain = MD5Digest.digest(uniqueKey)[0]
        return uniqueKey.copyOfRange(0, abs(obtain % uniqueKey.size))
    }

    fun encrypt(password: ByteArray, content: ByteArray): ByteArray {
        val randomByteArray: ByteArray = obtainUniqueKey()
        val digest = MD5Digest.digest(salt.plus(password).plus(randomByteArray))
        return AESDigest.encrypt(content, digest)
    }

    fun decrypt(password: ByteArray, content: ByteArray?): ByteArray? {
        if (content == null) {
            return null
        }
        val randomByteArray = obtainUniqueKey()
        val digest = MD5Digest.digest(salt.plus(password).plus(randomByteArray))
        return AESDigest.decrypt(content, digest)
    }

    interface StorageUniqueKey {
        fun obtainUniqueKey(): ByteArray
    }
}
