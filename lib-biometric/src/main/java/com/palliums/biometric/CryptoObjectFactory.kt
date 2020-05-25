package com.palliums.biometric

import androidx.annotation.RestrictTo
import com.palliums.biometric.crypto.CipherFactory
import com.palliums.biometric.crypto.MacFactory
import com.palliums.biometric.crypto.SignatureFactory

/**
 * Created by elephant on 2020/5/19 17:45.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Wrapper around different factories. It decides which factory should be used
 * when creating CryptoObject.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class CryptoObjectFactory(
    private val cipherFactory: CipherFactory?,
    private val macFactory: MacFactory?,
    private val signatureFactory: SignatureFactory?
) {

    fun createCryptoObject(mode: Mode, key: String): CryptoObject? {
        return when {
            cipherFactory != null -> {
                createCipherCryptoObject(mode, key)
            }

            macFactory != null -> {
                createMacCryptoObject(mode, key)
            }

            signatureFactory != null -> {
                createSignatureCryptoObject(mode, key)
            }

            else -> {
                null
            }
        }
    }

    private fun createCipherCryptoObject(
        mode: Mode,
        key: String
    ): CryptoObject? {
        val cipher = if (mode == Mode.ENCRYPTION)
            cipherFactory!!.createEncryptionCrypter(key)
        else
            cipherFactory!!.createDecryptionCrypter(key)
        return if (cipher == null) null else CryptoObject(
            cipher
        )
    }

    private fun createMacCryptoObject(
        mode: Mode,
        key: String
    ): CryptoObject? {
        val mac = if (mode == Mode.ENCRYPTION)
            macFactory!!.createEncryptionCrypter(key)
        else
            macFactory!!.createDecryptionCrypter(key)
        return if (mac == null) null else CryptoObject(mac)
    }

    private fun createSignatureCryptoObject(
        mode: Mode,
        key: String
    ): CryptoObject? {
        val signature = if (mode == Mode.ENCRYPTION)
            signatureFactory!!.createEncryptionCrypter(key)
        else
            signatureFactory!!.createDecryptionCrypter(key)
        return if (signature == null) null else CryptoObject(
            signature
        )
    }
}