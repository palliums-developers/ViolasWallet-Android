package com.palliums.biometric.crypto.impl

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties.*
import android.util.Base64
import androidx.annotation.RequiresApi
import com.palliums.biometric.crypto.CipherFactory
import java.security.Key
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec

/**
 * Created by elephant on 2020/5/19 16:19.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * AES Cipher implementation. By default the given Cipher is created with
 * Key which requires user authentication.
 * This implementation is used by default if other Factory is not provided.
 */
@RequiresApi(Build.VERSION_CODES.M)
open class AesCipherFactory(context: Context) : CipherFactory {

    companion object {
        private const val CIPHER_TRANSFORMATION =
            "$KEY_ALGORITHM_AES/${BLOCK_MODE_CBC}/${ENCRYPTION_PADDING_PKCS7}"
        private const val KEY_KEYSTORE = "AndroidKeyStore"
        private const val NAME_SHARED_PREFS = "biometric_iv"
    }

    private var keyGenerator: KeyGenerator? = null
    private var keyStore: KeyStore? = null
    private val sharedPrefs: SharedPreferences

    init {
        sharedPrefs = context.getSharedPreferences(NAME_SHARED_PREFS, Context.MODE_PRIVATE)

        try {
            keyStore = KeyStore.getInstance(KEY_KEYSTORE)
            keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM_AES, KEY_KEYSTORE)
        } catch (ignored: Exception) {
            /* Gracefully handle exception later when create method is invoked. */
        }
    }

    override fun createEncryptionCrypter(key: String): Cipher? {
        if (keyStore == null || keyGenerator == null) {
            return null
        }

        return try {
            val secureKey = createKey(key)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secureKey)
            saveIv(key, cipher.iv)
            cipher
        } catch (e: Exception) {
            null
        }
    }

    override fun createDecryptionCrypter(key: String): Cipher? {
        if (keyStore == null || keyGenerator == null) {
            return null
        }

        return try {
            val secureKey = loadKey(key)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val iv = loadIv(key)
            cipher.init(Cipher.DECRYPT_MODE, secureKey, IvParameterSpec(iv))
            cipher
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Simple method created to be easily extendable and create cipher which
     * does not require user authentication.
     */
    protected open fun isUserAuthRequired(): Boolean {
        return true
    }

    /**
     * Create secure key used to create Cipher.
     *
     * @param key name of the keystore.
     * @return created key, or null if something weird happens.
     * @throws Exception if anything fails, it is handled gracefully.
     */
    @Throws(java.lang.Exception::class)
    private fun createKey(key: String): Key {
        val keyGenParamsBuilder = KeyGenParameterSpec.Builder(
            key,
            PURPOSE_DECRYPT or PURPOSE_ENCRYPT
        )
            .setBlockModes(BLOCK_MODE_CBC)
            .setEncryptionPaddings(ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(isUserAuthRequired())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            keyGenParamsBuilder.setInvalidatedByBiometricEnrollment(isUserAuthRequired())
        }
        keyGenerator!!.init(keyGenParamsBuilder.build())
        keyGenerator!!.generateKey()
        return loadKey(key)
    }


    /**
     * Load IV from Shared preferences. Decode from Base64.
     */
    private fun loadIv(key: String): ByteArray {
        return Base64.decode(
            sharedPrefs.getString(key, ""),
            Base64.DEFAULT
        )
    }

    /**
     * Load [Key] from [KeyStore].
     *
     * @param key name of the [Key] to load.
     */
    @Throws(Exception::class)
    private fun loadKey(key: String): Key {
        keyStore!!.load(null)
        return keyStore!!.getKey(key, null)
    }

    /**
     * Save IV to Shared preferences. Before saving encode it to Base64.
     */
    private fun saveIv(key: String, iv: ByteArray?) {
        sharedPrefs.edit()
            .putString(key, Base64.encodeToString(iv, Base64.DEFAULT))
            .apply()
    }
}