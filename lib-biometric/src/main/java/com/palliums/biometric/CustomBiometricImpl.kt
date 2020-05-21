package com.palliums.biometric

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.palliums.biometric.custom.BiometricManager
import com.palliums.biometric.custom.BiometricPrompt
import com.palliums.biometric.exceptions.CryptoObjectInitException
import com.palliums.biometric.exceptions.InvalidParametersException
import com.palliums.biometric.exceptions.MissingHardwareException
import com.palliums.biometric.exceptions.NoEnrolledFingerprintException
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
class CustomBiometricImpl(
    context: Context,
    private val asyncCryptoObjectFactory: AsyncCryptoObjectFactory,
    private val crypterProxy: CrypterProxy
) : BiometricCompat {

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val executor by lazy { Executors.newSingleThreadExecutor() }
    private val biometricManager by lazy { BiometricManager.from(context) }

    private var asyncCryptoObjectFactoryCallback: AsyncCryptoObjectFactory.Callback? = null
    private var biometricPrompt: BiometricPrompt? = null
    private var biometricPromptCallback: CustomBiometricCallback? = null
    private var creatingCryptoObject = false

    override fun hasBiometricHardware(): Boolean {
        return biometricManager.canAuthenticate() != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
    }

    override fun hasEnrolledBiometric(): Boolean {
        return biometricManager.canAuthenticate() != BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    }

    override fun canAuthenticate(): Boolean {
        return biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
    }

    override fun authenticate(
        params: BiometricCompat.PromptParams,
        callback: BiometricCompat.Callback
    ) {
        if (preconditionsInvalid(params, Mode.AUTHENTICATION, null, null, callback)) return

        LogUtils.log("Starting authentication")
        startNativeBiometricAuthentication(
            params,
            Mode.AUTHENTICATION,
            null,
            null,
            callback,
            null
        )
    }

    override fun encrypt(
        params: BiometricCompat.PromptParams,
        key: String,
        value: String,
        callback: BiometricCompat.Callback
    ) {
        if (preconditionsInvalid(params, Mode.ENCRYPTION, key, value, callback)) return

        initializeCryptoObject(params, Mode.ENCRYPTION, key, value, callback)
    }

    override fun decrypt(
        params: BiometricCompat.PromptParams,
        key: String,
        value: String,
        callback: BiometricCompat.Callback
    ) {
        if (preconditionsInvalid(params, Mode.DECRYPTION, key, value, callback)) return

        initializeCryptoObject(params, Mode.DECRYPTION, key, value, callback)
    }

    override fun cancel() {
        biometricPrompt?.let {
            it.cancelAuthentication()
            biometricPrompt = null
        }

        biometricPromptCallback?.let {
            it.cancel()
            biometricPromptCallback = null
        }

        asyncCryptoObjectFactoryCallback?.let {
            it.cancel()
            asyncCryptoObjectFactoryCallback = null
        }
    }

    private fun initializeCryptoObject(
        params: BiometricCompat.PromptParams,
        mode: Mode,
        key: String,
        value: String,
        callback: BiometricCompat.Callback
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
                    callback.onError(CryptoObjectInitException())
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
        callback: BiometricCompat.Callback,
        cryptoObject: CryptoObject?
    ) {
        /*
         * Use proxy callback because some devices do not cancel authentication when error is received.
         * Cancel authentication manually and proxy the result to real callback.
         */
        biometricPromptCallback =
            CustomBiometricCallback(
                crypterProxy,
                mode,
                value,
                object : BiometricCompat.Callback {
                    override fun onResult(result: BiometricCompat.Result) {
                        if (result.type == BiometricCompat.Type.ERROR
                            || result.type == BiometricCompat.Type.SUCCESS
                        ) {
                            cancel()
                        }
                        callback.onResult(result)
                    }

                    override fun onError(e: Exception) {
                        cancel()
                        callback.onError(e)
                    }
                }
            )

        if (params.dialogOwner is Fragment) {
            biometricPrompt =
                BiometricPrompt(params.dialogOwner, executor, biometricPromptCallback!!)
        }
        if (params.dialogOwner is FragmentActivity) {
            biometricPrompt =
                BiometricPrompt(params.dialogOwner, executor, biometricPromptCallback!!)
        }

        /* Delay with post because Navigation and Prompt both work with Fragment transactions */
        mainHandler.post {
            if (biometricPrompt == null) return@post

            if (mode == Mode.AUTHENTICATION) {
                /* Simple Authentication call */
                LogUtils.log("Starting authentication")
                callback.onResult(
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
                callback.onResult(
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
        if (!params.deviceCredentialsAllowed) {
            builder.setNegativeButtonText(params.negativeButtonText!!)
        }
        return builder.build()
    }

    private fun preconditionsInvalid(
        params: BiometricCompat.PromptParams,
        mode: Mode,
        key: String?,
        value: String?,
        callback: BiometricCompat.Callback
    ): Boolean {
        if ((biometricPromptCallback != null && biometricPromptCallback!!.isAuthenticationActive())
            || creatingCryptoObject
        ) {
            LogUtils.log("Authentication is already active. Ignoring authenticate call.")
            return true
        }

        if (!hasBiometricHardware()) {
            callback.onError(MissingHardwareException())
            return true
        }

        if (!hasEnrolledBiometric()) {
            callback.onError(NoEnrolledFingerprintException())
            return true
        }

        val promptParamsErrors = validatePromptParams(mode, params)
        if (promptParamsErrors.isNotEmpty()) {
            callback.onError(InvalidParametersException(promptParamsErrors))
            return true
        }

        val cipherParamsErrors = validateCipherParams(mode, key, value)
        if (cipherParamsErrors.isNotEmpty()) {
            callback.onError(InvalidParametersException(cipherParamsErrors))
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