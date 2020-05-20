package com.palliums.biometric

import android.os.Handler
import android.os.Looper
import androidx.biometric.BiometricConstants
import androidx.biometric.BiometricPrompt
import com.palliums.biometric.exceptions.DecryptionException
import com.palliums.biometric.exceptions.EncryptionException

/**
 * Created by elephant on 2020/5/20 10:51.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Extended default callback.
 * Tracks if the authentication is still active and handles multiple
 * edge cases that are not expected by the user.
 */
class BiometricPromptCallback(
    private val crypterProxy: CrypterProxy,
    private val mode: Mode,
    private val value: String?,
    private val callback: BiometricCompat.Callback
) : BiometricPrompt.AuthenticationCallback() {

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private var isAuthenticationActive = true

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        if (!isAuthenticationActive) return

        isAuthenticationActive = false
        val reason = errorToReason(errorCode)
        mainHandler.post {
            callback.onResult(
                BiometricCompat.Result(
                    BiometricCompat.Type.ERROR,
                    reason,
                    null,
                    errString.toString()
                )
            )
        }
    }

    override fun onAuthenticationFailed() {
        if (!isAuthenticationActive) return

        mainHandler.post {
            callback.onResult(
                BiometricCompat.Result(
                    BiometricCompat.Type.INFO,
                    BiometricCompat.Reason.AUTHENTICATION_FAIL
                )
            )
        }
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        if (!isAuthenticationActive) return

        isAuthenticationActive = false
        when {
            mode == Mode.AUTHENTICATION -> {
                mainHandler.post {
                    callback.onResult(
                        BiometricCompat.Result(
                            BiometricCompat.Type.SUCCESS,
                            BiometricCompat.Reason.AUTHENTICATION_SUCCESS
                        )
                    )
                }
            }

            result.cryptoObject == null -> {
                mainHandler.post {
                    callback.onResult(
                        BiometricCompat.Result(
                            BiometricCompat.Type.ERROR,
                            BiometricCompat.Reason.UNKNOWN,
                            null,
                            "BiometricPrompt.CryptoObject is null"
                        )
                    )
                }
            }

            else -> {
                cipherValue(result.cryptoObject!!)
            }
        }
    }

    /**
     * Cancel Goldfinger authentication.
     *
     *
     * Native authentication will invoke [onAuthenticationError]
     * but the error will be ignored because the user knowingly canceled the authentication.
     */
    fun cancel() {
        isAuthenticationActive = false
    }

    fun isAuthenticationActive(): Boolean {
        return isAuthenticationActive
    }

    /**
     * Cipher the value with unlocked [BiometricPrompt.CryptoObject].
     *
     * @param cryptoObject unlocked [BiometricPrompt.CryptoObject] that is ready to use.
     */
    private fun cipherValue(cryptoObject: BiometricPrompt.CryptoObject) {
        val cipheredValue = if (mode == Mode.ENCRYPTION)
            crypterProxy.encrypt(cryptoObject, value!!)
        else
            crypterProxy.decrypt(cryptoObject, value!!)

        mainHandler.post {
            if (cipheredValue != null) {
                callback.onResult(
                    BiometricCompat.Result(
                        BiometricCompat.Type.SUCCESS,
                        BiometricCompat.Reason.AUTHENTICATION_SUCCESS,
                        cipheredValue,
                        null
                    )
                )
            } else {
                callback.onError(
                    if (mode == Mode.ENCRYPTION)
                        EncryptionException()
                    else
                        DecryptionException()
                )
            }
        }
    }

    private fun errorToReason(errorId: Int): BiometricCompat.Reason {
        return when (errorId) {
            BiometricConstants.ERROR_HW_UNAVAILABLE ->
                BiometricCompat.Reason.HW_UNAVAILABLE
            BiometricConstants.ERROR_UNABLE_TO_PROCESS ->
                BiometricCompat.Reason.UNABLE_TO_PROCESS
            BiometricConstants.ERROR_TIMEOUT ->
                BiometricCompat.Reason.TIMEOUT
            BiometricConstants.ERROR_NO_SPACE ->
                BiometricCompat.Reason.NO_SPACE
            BiometricConstants.ERROR_CANCELED ->
                BiometricCompat.Reason.CANCELED
            BiometricConstants.ERROR_LOCKOUT ->
                BiometricCompat.Reason.LOCKOUT
            BiometricConstants.ERROR_VENDOR ->
                BiometricCompat.Reason.VENDOR
            BiometricConstants.ERROR_LOCKOUT_PERMANENT ->
                BiometricCompat.Reason.LOCKOUT_PERMANENT
            BiometricConstants.ERROR_USER_CANCELED ->
                BiometricCompat.Reason.USER_CANCELED
            BiometricConstants.ERROR_NO_BIOMETRICS ->
                BiometricCompat.Reason.NO_BIOMETRICS
            BiometricConstants.ERROR_HW_NOT_PRESENT ->
                BiometricCompat.Reason.HW_NOT_PRESENT
            BiometricConstants.ERROR_NEGATIVE_BUTTON ->
                BiometricCompat.Reason.NEGATIVE_BUTTON
            BiometricConstants.ERROR_NO_DEVICE_CREDENTIAL ->
                BiometricCompat.Reason.NO_DEVICE_CREDENTIAL
            else ->
                BiometricCompat.Reason.UNKNOWN
        }
    }
}