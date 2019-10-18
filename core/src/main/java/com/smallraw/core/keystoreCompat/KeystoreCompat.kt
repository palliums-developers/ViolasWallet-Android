package com.smallraw.core.keystoreCompat

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import java.math.BigInteger
import java.security.*
import java.security.cert.Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import javax.crypto.Cipher
import javax.security.auth.x500.X500Principal


class KeystoreCompat(private val context: Context, private val mAlias: String = "default") {
    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    }

    private val keyStore: KeyStore

    init {
        keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        createNewSecretKey(mAlias)
    }

    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class,
        InvalidAlgorithmParameterException::class
    )
    private fun createNewSecretKey(alias: String) {
        if (!keyStore.containsAlias(alias)) {
            val start = GregorianCalendar()
            val end = GregorianCalendar()
            end.add(Calendar.YEAR, 10)

            val spec = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                generateKeyPair(context, start, end)
            } else {
                generateKeyPairByM(start, end)
            }

            val kpGenerator = KeyPairGenerator.getInstance(
                "RSA",
                ANDROID_KEY_STORE
            )
            kpGenerator.initialize(spec)
            kpGenerator.generateKeyPair()
        }
    }

    private fun getCipher(): Cipher {
        try {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Cipher.getInstance("RSA/ECB/PKCS1Padding")
            } else {
                Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidKeyStoreBCWorkaround")
            }
        } catch (exception: Exception) {
            throw RuntimeException("getCipher: Failed to get an instance of Cipher", exception)
        }

    }

    fun encrypt(content: ByteArray): ByteArray? {
        val cipher = getCipher()
        cipher.init(Cipher.ENCRYPT_MODE, keyStore.getCertificate(mAlias).publicKey)
        return cipher.doFinal(content)
    }

    fun decrypt(content: ByteArray): ByteArray? {
        val cipher = getCipher()
        cipher.init(Cipher.DECRYPT_MODE, keyStore.getKey(mAlias, null))
        return cipher.doFinal(content)
    }

    @Suppress("DEPRECATION")
    private fun generateKeyPair(
        context: Context,
        start: GregorianCalendar,
        end: GregorianCalendar
    ): KeyPairGeneratorSpec {
        return KeyPairGeneratorSpec.Builder(context)
            .setAlias(mAlias)
            .setSubject(X500Principal("CN=$mAlias"))
            .setSerialNumber(BigInteger.TEN)
            .setStartDate(start.time)
            .setEndDate(end.time)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun generateKeyPairByM(
        start: GregorianCalendar,
        end: GregorianCalendar
    ): KeyGenParameterSpec {
        return KeyGenParameterSpec.Builder(
            mAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setCertificateSerialNumber(BigInteger.TEN)
            .setCertificateSubject(X500Principal("CN=$mAlias"))
            .setCertificateNotBefore(start.time)
            .setCertificateNotAfter(end.time)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
    }
}
