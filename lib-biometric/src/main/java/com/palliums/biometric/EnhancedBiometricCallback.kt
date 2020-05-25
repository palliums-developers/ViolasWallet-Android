package com.palliums.biometric

import android.os.Handler
import android.os.Looper
import androidx.annotation.RestrictTo
import androidx.biometric.enhanced.BiometricConstants
import androidx.biometric.enhanced.BiometricPrompt
import com.palliums.biometric.exceptions.DecryptionException
import com.palliums.biometric.exceptions.EncryptionException
import com.palliums.biometric.util.LogUtils

/**
 * Created by elephant on 2020/5/20 10:51.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Extended default callback.
 * Tracks if the authentication is still active and handles multiple
 * edge cases that are not expected by the user.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class EnhancedBiometricCallback(
    private val crypterProxy: CrypterProxy,
    private val mode: Mode,
    private val value: String?,
    private val callback: (BiometricCompat.Result) -> Unit
) : BiometricPrompt.AuthenticationCallback() {

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private var isAuthenticationActive = true

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        if (!isAuthenticationActive) return

        isAuthenticationActive = false
        val reason = errorToReason(errorCode)
        LogUtils.log("onAuthenticationError [$reason, $errString]")
        mainHandler.post {
            callback.invoke(
                BiometricCompat.Result(
                    BiometricCompat.Type.ERROR,
                    reason,
                    message = errString.toString()
                )
            )
        }
    }

    override fun onAuthenticationFailed() {
        if (!isAuthenticationActive) return

        val reason = BiometricCompat.Reason.AUTHENTICATION_FAIL
        LogUtils.log("onAuthenticationFailed [$reason]")
        mainHandler.post {
            callback.invoke(
                BiometricCompat.Result(
                    BiometricCompat.Type.INFO,
                    reason
                )
            )
        }
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        if (!isAuthenticationActive) return

        isAuthenticationActive = false
        when {
            mode == Mode.AUTHENTICATION -> {
                LogUtils.log("onAuthenticationSucceeded")
                mainHandler.post {
                    callback.invoke(
                        BiometricCompat.Result(
                            BiometricCompat.Type.SUCCESS,
                            BiometricCompat.Reason.AUTHENTICATION_SUCCESS
                        )
                    )
                }
            }

            result.cryptoObject == null -> {
                val reason = BiometricCompat.Reason.UNKNOWN
                val message = "androidx.biometric.enhanced.BiometricPrompt.CryptoObject is null"
                LogUtils.log("onAuthenticationError [$reason, $message]")
                mainHandler.post {
                    callback.invoke(
                        BiometricCompat.Result(
                            BiometricCompat.Type.ERROR,
                            reason,
                            message = message
                        )
                    )
                }
            }

            else -> {
                LogUtils.log("onAuthenticationSucceeded")
                cipherValue(convertCryptoObject(result.cryptoObject!!))
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
     * Cipher the value with unlocked [CryptoObject].
     *
     * @param cryptoObject unlocked [CryptoObject] that is ready to use.
     */
    private fun cipherValue(cryptoObject: CryptoObject) {
        val cipheredValue = if (mode == Mode.ENCRYPTION)
            crypterProxy.encrypt(cryptoObject, value!!)
        else
            crypterProxy.decrypt(cryptoObject, value!!)

        mainHandler.post {
            if (cipheredValue != null) {
                callback.invoke(
                    BiometricCompat.Result(
                        BiometricCompat.Type.SUCCESS,
                        BiometricCompat.Reason.AUTHENTICATION_SUCCESS,
                        value = cipheredValue
                    )
                )
            } else {
                callback.invoke(
                    BiometricCompat.Result(
                        BiometricCompat.Type.ERROR,
                        BiometricCompat.Reason.AUTHENTICATION_EXCEPTION,
                        exception = if (mode == Mode.ENCRYPTION)
                            EncryptionException()
                        else
                            DecryptionException()
                    )
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
            BiometricConstants.ERROR_POSITIVE_BUTTON ->
                BiometricCompat.Reason.POSITIVE_BUTTON
            else ->
                BiometricCompat.Reason.UNKNOWN
        }
    }

    private fun convertCryptoObject(cryptoObject: BiometricPrompt.CryptoObject): CryptoObject {
        return when {
            cryptoObject.signature != null -> CryptoObject(cryptoObject.signature!!)
            cryptoObject.cipher != null -> CryptoObject(cryptoObject.cipher!!)
            else -> CryptoObject(cryptoObject.mac!!)
        }
    }
}