package com.smallraw.core

import com.smallraw.core.crypto.AESDigest
import org.junit.Test

import org.junit.Assert.*

class AESUnitTest {
    @Test
    fun testAES_encode_decode() {
        val content = "abc".toByteArray()
        val seed = "123123123123123123123123".toByteArray()
        val encrypt = AESDigest.encrypt(content, seed)
        val decrypt = AESDigest.decrypt(encrypt, seed)
        assertArrayEquals(content, decrypt)
    }

    @Test
    fun test_gen_key() {
        val rawKey1 = AESDigest.generaKey("123".toByteArray())
        val rawKey2 = AESDigest.generaKey("123".toByteArray())
        assertArrayEquals(rawKey1, rawKey2)
    }
}
