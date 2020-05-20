package com.palliums.biometric

import androidx.biometric.BiometricPrompt
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
 */
class CryptoObjectFactory(
    private val cipherFactory: CipherFactory?,
    private val macFactory: MacFactory?,
    private val signatureFactory: SignatureFactory?
) {

    fun createCryptoObject(key: String, mode: Mode): BiometricPrompt.CryptoObject? {
        return when {
            cipherFactory != null -> {
                createCipherCryptoObject(key, mode)
            }

            macFactory != null -> {
                createMacCryptoObject(key, mode)
            }

            signatureFactory != null -> {
                createSignatureCryptoObject(key, mode)
            }

            else -> {
                null
            }
        }
    }

    private fun createCipherCryptoObject(
        key: String,
        mode: Mode
    ): BiometricPrompt.CryptoObject? {
        val cipher = if (mode == Mode.ENCRYPTION)
            cipherFactory!!.createEncryptionCrypter(key)
        else
            cipherFactory!!.createDecryptionCrypter(key)
        return if (cipher == null) null else BiometricPrompt.CryptoObject(cipher)
    }

    private fun createMacCryptoObject(
        key: String,
        mode: Mode
    ): BiometricPrompt.CryptoObject? {
        val mac = if (mode == Mode.ENCRYPTION)
            macFactory!!.createEncryptionCrypter(key)
        else
            macFactory!!.createDecryptionCrypter(key)
        return if (mac == null) null else BiometricPrompt.CryptoObject(mac)
    }

    private fun createSignatureCryptoObject(
        key: String,
        mode: Mode
    ): BiometricPrompt.CryptoObject? {
        val signature = if (mode == Mode.ENCRYPTION)
            signatureFactory!!.createEncryptionCrypter(key)
        else
            signatureFactory!!.createDecryptionCrypter(key)
        return if (signature == null) null else BiometricPrompt.CryptoObject(signature)
    }
}