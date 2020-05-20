package com.palliums.biometric.crypto.impl

import android.util.Base64
import com.palliums.biometric.crypto.CipherCrypter
import javax.crypto.Cipher

/**
 * Created by elephant on 2020/5/19 17:14.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Implementation uses unlocked cipher to encrypt or decrypt the data.
 * Used by default if other Crypter implementation is not used.
 *
 * @see CipherCrypter
 */
class Base64CipherCrypter : CipherCrypter {

    override fun encrypt(crypter: Cipher, value: String): String? {
        return try {
            val encryptedBytes = crypter.doFinal(value.toByteArray())
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    override fun decrypt(crypter: Cipher, value: String): String? {
        return try {
            val decodedBytes = Base64.decode(value, Base64.NO_WRAP)
            String(crypter.doFinal(decodedBytes))
        } catch (e: Exception) {
            null
        }
    }
}