package com.smallraw.core

import com.smallraw.core.crypto.MD5Digest
import org.junit.Test

class Md5UnitTest {
    @Test
    fun test_Hash(){
        val content = "123".toByteArray()

        MD5Digest.digest(content)
    }
}