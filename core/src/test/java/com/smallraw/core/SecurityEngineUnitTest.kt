package com.smallraw.core

import com.smallraw.core.crypto.SecurityEngine
import org.junit.Assert
import org.junit.Test
import java.security.SecureRandom

class SecurityEngineUnitTest {
    @Test
    fun test_encode() {
        val password = "123123".toByteArray()
        val content = "abc".toByteArray()
        val securityEngine = SecurityEngine()
        val encrypt = securityEngine.encrypt(password, content)
        val decrypt = securityEngine.decrypt(password, encrypt)
        Assert.assertArrayEquals(content, decrypt)
    }

    @Test
    fun test_encode_parameter() {
        val byteArray = ByteArray(128)
        SecureRandom.getInstance("SHA1PRNG").nextBytes(byteArray)

        val password = "123123".toByteArray()
        val content = "[nothing, tell, toilet, filter, tonight, dress, quality, flat, humble, zoo, must, stable]".toByteArray()

        val securityEngine = SecurityEngine(object : SecurityEngine.StorageUniqueKey {
            override fun obtainUniqueKey(): ByteArray {
                return byteArray
            }
        })

        val securityEngine1 = SecurityEngine(object : SecurityEngine.StorageUniqueKey {
            override fun obtainUniqueKey(): ByteArray {
                return byteArray
            }
        })
        val encrypt = securityEngine.encrypt(password, content)
        val decrypt = securityEngine1.decrypt(password, encrypt)
        Assert.assertArrayEquals(content, decrypt)
    }
}