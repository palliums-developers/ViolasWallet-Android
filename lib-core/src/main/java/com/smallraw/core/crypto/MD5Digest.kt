package com.smallraw.core.crypto

import java.security.MessageDigest

class MD5Digest {

    companion object {
        fun digest(content: ByteArray, offset: Int = 0, size: Int = content.size): ByteArray {
            val messageDigest = MessageDigest.getInstance("MD5")
            messageDigest.update(content, offset, size)
            return messageDigest.digest()
        }
    }
}
