package com.palliums.biometric

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RestrictTo
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.biometric.enhanced.BiometricManager
import androidx.biometric.enhanced.BiometricPrompt
import androidx.biometric.enhanced.Utils
import com.palliums.biometric.exceptions.*
import com.palliums.biometric.util.LogUtils
import java.util.concurrent.Executors

/**
 * Created by elephant on 2020/5/20 14:36.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * BiometricCompat implementation for Android Marshmallow and newer.
 * Older versions use [BiometricMock].
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class EnhancedBiometricImpl(
    private val context: Context,
    private val asyncCryptoObjectFactory: AsyncCryptoObjectFactory,
    private val crypterProxy: CrypterProxy
) : BiometricCompat {

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val executor by lazy { Executors.newSingleThreadExecutor() }
    private val biometricManager by lazy { BiometricManager.from(context) }
    private val fingerprintManager by lazy { FingerprintManagerCompat.from(context) }

    private var asyncCryptoObjectFactoryCallback: AsyncCryptoObjectFactory.Callback? = null
    private var biometricPrompt: BiometricPrompt? = null
    private var biometricCallback: EnhancedBiometricCallback? = null
    private var creatingCryptoObject = false

    override fun canAuthenticate(useFingerprint: Boolean): Int {
        return if (useFingerprint
            || Utils.shouldUseFingerprintForCrypto(context, Build.MANUFACTURER, Build.MODEL)
        ) {
            if (!fingerprintManager.isHardwareDetected) {
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE;
            } else if (!fingerprintManager.hasEnrolledFingerprints()) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED;
            } else {
                BiometricManager.BIOMETRIC_SUCCESS;
            }
        } else {
            biometricManager.canAuthenticate()
        }
    }

    override fun authenticate(
        params: BiometricCompat.PromptParams,
        callback: (BiometricCompat.Result) -> Unit
    ) {
        if (preconditionsInvalid(params, Mode.AUTHENTICATION, null, null, callback)) return

        startNativeBiometricAuthentication(params, Mode.AUTHENTICATION, null, null, callback, null)
    }

    override fun encrypt(
        params: BiometricCompat.PromptParams,
        key: String,
        value: String,
        callback: (BiometricCompat.Result) -> Unit
    ) {
        if (preconditionsInvalid(params, Mode.ENCRYPTION, key, value, callback)) return

        initializeCryptoObject(params, Mode.ENCRYPTION, key, value, callback)
    }

    override fun decrypt(
        params: BiometricCompat.PromptParams,
        key: String,
        value: String,
        callback: (BiometricCompat.Result) -> Unit
    ) {
        if (preconditionsInvalid(params, Mode.DECRYPTION, key, value, callback)) return

        initializeCryptoObject(params, Mode.DECRYPTION, key, value, callback)
    }

    override fun cancel() {
        if (biometricPrompt != null) {
            biometricPrompt!!.cancelAuthentication()
            biometricPrompt = null
        }

        if (biometricCallback != null) {
            biometricCallback!!.cancel()
            biometricCallback = null
        }

        if (asyncCryptoObjectFactoryCallback != null) {
            asyncCryptoObjectFactoryCallback!!.cancel()
            asyncCryptoObjectFactoryCallback = null
        }
    }

    private fun initializeCryptoObject(
        params: BiometricCompat.PromptParams,
        mode: Mode,
        key: String,
        value: String,
        callback: (BiometricCompat.Result) -> Unit
    ) {
        LogUtils.log("Creating CryptoObject")
        asyncCryptoObjectFactoryCallback = object : AsyncCryptoObjectFactory.Callback() {

            override fun onCryptoObjectCreated(cryptoObject: CryptoObject?) {
                creatingCryptoObject = false
                if (cryptoObject != null) {
                    startNativeBiometricAuthentication(
                        params,
                        mode,
                        key,
                        value,
                        callback,
                        cryptoObject
                    )
                } else {
                    LogUtils.log("Failed to create CryptoObject")
                    callback.invoke(
                        BiometricCompat.Result(
                            BiometricCompat.Type.ERROR,
                            BiometricCompat.Reason.AUTHENTICATION_EXCEPTION,
                            exception = CryptoObjectInitException()
                        )
                    )
                }
            }
        }

        creatingCryptoObject = true
        asyncCryptoObjectFactory.createCryptoObject(mode, key, asyncCryptoObjectFactoryCallback!!)
    }

    private fun startNativeBiometricAuthentication(
        params: BiometricCompat.PromptParams,
        mode: Mode,
        key: String?,
        value: String?,
        callback: (BiometricCompat.Result) -> Unit,
        cryptoObject: CryptoObject?
    ) {
        /*
         * Use proxy callback because some devices do not cancel authentication when error is received.
         * Cancel authentication manually and proxy the result to real callback.
         */
        biometricCallback = EnhancedBiometricCallback(
            crypterProxy,
            mode,
            value,
            params.reactivateWhenLockoutPermanent,
            params.autoCloseWhenError
        ) {
            if (it.type == BiometricCompat.Type.ERROR || it.type == BiometricCompat.Type.SUCCESS) {
                cancel()
            }
            callback.invoke(it)
        }

        if (params.dialogOwner is Fragment) {
            biometricPrompt =
                BiometricPrompt(
                    params.dialogOwner,
                    executor,
                    biometricCallback!!
                )
        }
        if (params.dialogOwner is FragmentActivity) {
            biometricPrompt =
                BiometricPrompt(
                    params.dialogOwner,
                    executor,
                    biometricCallback!!
                )
        }

        /* Delay with post because Navigation and Prompt both work with Fragment transactions */
        mainHandler.post {
            if (biometricPrompt == null) return@post

            if (mode == Mode.AUTHENTICATION) {
                /* Simple Authentication call */
                LogUtils.log("Starting authentication")
                callback.invoke(
                    BiometricCompat.Result(
                        BiometricCompat.Type.INFO,
                        BiometricCompat.Reason.AUTHENTICATION_START
                    )
                )
                biometricPrompt!!.authenticate(buildPromptInfo(params))
            } else {
                /* Encryption/Decryption call with initialized CryptoObject */
                /* Encryption/Decryption call with initialized CryptoObject */
                LogUtils.log("Starting authentication [keyName=$key; value=$value]")
                callback.invoke(
                    BiometricCompat.Result(
                        BiometricCompat.Type.INFO,
                        BiometricCompat.Reason.AUTHENTICATION_START
                    )
                )
                biometricPrompt!!.authenticate(
                    buildPromptInfo(params),
                    convertCryptoObject(cryptoObject!!)
                )
            }
        }
    }

    private fun convertCryptoObject(cryptoObject: CryptoObject): BiometricPrompt.CryptoObject {
        return when {
            cryptoObject.signature != null -> BiometricPrompt.CryptoObject(cryptoObject.signature)
            cryptoObject.cipher != null -> BiometricPrompt.CryptoObject(cryptoObject.cipher)
            else -> BiometricPrompt.CryptoObject(cryptoObject.mac!!)
        }
    }

    /**
     * Create new [BiometricPrompt.PromptInfo] instance. Parameter
     * validation is done earlier in the code so we can trust the data at
     * this step.
     */
    private fun buildPromptInfo(
        params: BiometricCompat.PromptParams
    ): BiometricPrompt.PromptInfo {
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(params.title!!)
            .setSubtitle(params.subtitle)
            .setDescription(params.description)
            .setDeviceCredentialAllowed(params.deviceCredentialsAllowed)
            .setConfirmationRequired(params.confirmationRequired)
            .setUseFingerprint(params.useFingerprint)
            .setReactivateWhenLockoutPermanent(params.reactivateWhenLockoutPermanent)
            .setAutoCloseWhenError(params.autoCloseWhenError)
        if (!params.deviceCredentialsAllowed) {
            builder.setNegativeButtonText(params.negativeButtonText!!)
        }
        if (!params.positiveButtonText.isNullOrBlank()) {
            builder.setPositiveButtonText(params.positiveButtonText)
        }
        if (params.customFingerprintDialogClass != null) {
            builder.setCustomFingerprintDialogClass(params.customFingerprintDialogClass)
        }
        return builder.build()
    }

    private fun preconditionsInvalid(
        params: BiometricCompat.PromptParams,
        mode: Mode,
        key: String?,
        value: String?,
        callback: (BiometricCompat.Result) -> Unit
    ): Boolean {
        if ((biometricCallback != null && biometricCallback!!.isAuthenticationActive())
            || creatingCryptoObject
        ) {
            LogUtils.log("Authentication is already active. Ignoring authenticate call.")
            return true
        }

        when (canAuthenticate(params.useFingerprint)) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                callback.invoke(
                    BiometricCompat.Result(
                        BiometricCompat.Type.ERROR,
                        BiometricCompat.Reason.AUTHENTICATION_EXCEPTION,
                        exception = MissingHardwareException()
                    )
                )
                return true
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                callback.invoke(
                    BiometricCompat.Result(
                        BiometricCompat.Type.ERROR,
                        BiometricCompat.Reason.AUTHENTICATION_EXCEPTION,
                        exception = NoEnrolledBiometricsException()
                    )
                )
                return true
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                callback.invoke(
                    BiometricCompat.Result(
                        BiometricCompat.Type.ERROR,
                        BiometricCompat.Reason.AUTHENTICATION_EXCEPTION,
                        exception = HardwareUnavailableException()
                    )
                )
                return true
            }
        }

        val promptParamsErrors = validatePromptParams(mode, params)
        if (promptParamsErrors.isNotEmpty()) {
            callback.invoke(
                BiometricCompat.Result(
                    BiometricCompat.Type.ERROR,
                    BiometricCompat.Reason.AUTHENTICATION_EXCEPTION,
                    exception = InvalidParametersException(promptParamsErrors)
                )
            )
            return true
        }

        val cipherParamsErrors = validateCipherParams(mode, key, value)
        if (cipherParamsErrors.isNotEmpty()) {
            callback.invoke(
                BiometricCompat.Result(
                    BiometricCompat.Type.ERROR,
                    BiometricCompat.Reason.AUTHENTICATION_EXCEPTION,
                    exception = InvalidParametersException(cipherParamsErrors)
                )
            )
            return true
        }

        return false
    }

    private fun validatePromptParams(
        mode: Mode,
        params: BiometricCompat.PromptParams
    ): List<String> {
        val errors = mutableListOf<String>()

        if (params.dialogOwner !is Fragment && params.dialogOwner !is FragmentActivity) {
            errors.add("DialogOwner must be of instance Fragment or FragmentActivity.")
        }

        if (params.title.isNullOrBlank()) {
            errors.add("Title is required!")
        }

        if (!params.deviceCredentialsAllowed && params.negativeButtonText.isNullOrBlank()) {
            errors.add("NegativeButtonText is required!")
        }

        if (params.deviceCredentialsAllowed && mode != Mode.AUTHENTICATION) {
            errors.add("DeviceCredentials are allowed only for BiometricCompat#authenticate method.")
        }

        return errors
    }

    private fun validateCipherParams(
        mode: Mode,
        key: String?,
        value: String?
    ): List<String> {
        val errors = mutableListOf<String>()

        if (mode != Mode.AUTHENTICATION) {
            if (key.isNullOrBlank()) {
                errors.add("Key is required if encryption or decryption is used!")
            }

            if (value.isNullOrBlank()) {
                errors.add("Value is required if encryption or decryption is used!")
            }
        }

        return errors
    }
}