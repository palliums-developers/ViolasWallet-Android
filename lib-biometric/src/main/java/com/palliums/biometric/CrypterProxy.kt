package com.palliums.biometric

import androidx.annotation.RestrictTo
import com.palliums.biometric.crypto.CipherCrypter
import com.palliums.biometric.crypto.MacCrypter
import com.palliums.biometric.crypto.SignatureCrypter

/**
 * Created by elephant on 2020/5/19 18:34.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Internal wrapper around different crypters to have this logic
 * in one place hidden from the rest of the code.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class CrypterProxy(
    private val cipherCrypter: CipherCrypter?,
    private val macCrypter: MacCrypter?,
    private val signatureCrypter: SignatureCrypter?
) {

    fun encrypt(cryptoObject: CryptoObject, value: String): String? {
        val cipher = cryptoObject.cipher
        if (cipher != null && cipherCrypter != null) {
            return cipherCrypter.encrypt(cipher, value)
        }

        val mac = cryptoObject.mac
        if (mac != null && macCrypter != null) {
            return macCrypter.encrypt(mac, value)
        }

        val signature = cryptoObject.signature
        if (signature != null && signatureCrypter != null) {
            return signatureCrypter.encrypt(signature, value)
        }

        return null
    }

    fun decrypt(cryptoObject: CryptoObject, value: String): String? {
        val cipher = cryptoObject.cipher
        if (cipher != null && cipherCrypter != null) {
            return cipherCrypter.decrypt(cipher, value)
        }

        val mac = cryptoObject.mac
        if (mac != null && macCrypter != null) {
            return macCrypter.decrypt(mac, value)
        }

        val signature = cryptoObject.signature
        if (signature != null && signatureCrypter != null) {
            return signatureCrypter.decrypt(signature, value)
        }

        return null
    }
}