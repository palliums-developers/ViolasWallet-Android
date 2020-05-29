package com.palliums.biometric

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.palliums.biometric.crypto.*
import com.palliums.biometric.crypto.impl.AesCipherFactory
import com.palliums.biometric.crypto.impl.Base64CipherCrypter
import androidx.biometric.enhanced.BaseFingerprintDialogFragment
import com.palliums.biometric.util.LogUtils

/**
 * Created by elephant on 2020/5/19 17:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * @see androidx.biometric.BiometricManager
 * @see androidx.biometric.BiometricPrompt
 * @see androidx.biometric.enhanced.BiometricManager
 * @see androidx.biometric.enhanced.BiometricPrompt
 * @see <a href="https://github.com/infinum/Android-Goldfinger">link</a>
 */
interface BiometricCompat {

    /**
     * @param useFingerprint true: 使用指纹识别; false: 使用生物特征识别（指纹、面部、虹膜）
     * @see [BiometricManager.canAuthenticate]
     */
    fun canAuthenticate(useFingerprint: Boolean = true): Int

    /**
     * Authenticate user via Fingerprint.
     * <p>
     * Example - Process payment after successful fingerprint authentication.
     *
     * @param params   parameters used to build [BiometricPrompt] instance
     * @param callback Returns fingerprint result and will be invoked multiple times during
     *                 fingerprint authentication as not all fingerprint results complete
     *                 the authentication.
     *
     *                 Result callback invoked for every fingerprint result (success, exception,
     *                 error or info).
     *                 It can be invoked multiple times during single fingerprint authentication.
     * @see BiometricCompat.Result
     */
    fun authenticate(
        params: PromptParams,
        callback: (Result) -> Unit
    )

    /**
     * Authenticate user via Fingerprint. If user is successfully authenticated,
     * [CrypterProxy] implementation is used to automatically encrypt given value.
     * <p>
     * Use it when saving some data that should not be saved as plain text (e.g. password).
     * To decrypt the value use [BiometricCompat.decrypt] method.
     * <p>
     * Example - Allow auto-login via Fingerprint.
     *
     * @param params   parameters used to build [BiometricPrompt] instance
     * @param key      unique key identifier, used to store cipher IV internally
     * @param value    String value which will be encrypted if user successfully authenticates
     * @param callback Returns fingerprint result and will be invoked multiple times during
     *                 fingerprint authentication as not all fingerprint results complete
     *                 the authentication.
     *
     *                 Result callback invoked for every fingerprint result (success, exception,
     *                 error or info).
     *                 It can be invoked multiple times during single fingerprint authentication.
     * @see BiometricCompat.Result
     */
    fun encrypt(
        params: PromptParams,
        key: String,
        value: String,
        callback: (Result) -> Unit
    )

    /**
     * Authenticate user via Fingerprint. If user is successfully authenticated,
     * [CrypterProxy] implementation is used to automatically decrypt given value.
     * <p>
     * Should be used together with [BiometricCompat.encrypt] to decrypt saved data.
     *
     * @param params   parameters used to build [BiometricPrompt] instance
     * @param key   unique key identifier, used to load Cipher IV internally
     * @param value String value which will be decrypted if user successfully authenticates
     * @param callback Returns fingerprint result and will be invoked multiple times during
     *                 fingerprint authentication as not all fingerprint results complete
     *                 the authentication.
     *
     *                 Result callback invoked for every fingerprint result (success, exception,
     *                 error or info).
     *                 It can be invoked multiple times during single fingerprint authentication.
     * @see BiometricCompat.Result
     */
    fun decrypt(
        params: PromptParams,
        key: String,
        value: String,
        callback: (Result) -> Unit
    )

    /**
     * Cancel current active Fingerprint authentication.
     */
    fun cancel()

    /**
     * Become Bob the builder.
     */
    class Builder(private val context: Context) {
        private var cipherFactory: CipherFactory? = null
        private var macFactory: MacFactory? = null
        private var signatureFactory: SignatureFactory? = null
        private var cipherCrypter: CipherCrypter? = null
        private var macCrypter: MacCrypter? = null
        private var signatureCrypter: SignatureCrypter? = null

        /**
         * @param useEnhanced true: 使用系统生物识别库的加强版; false: 使用系统生物识别库
         */
        fun build(useEnhanced: Boolean = true): BiometricCompat {
            ensureParamsValid()
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                buildBiometric(useEnhanced)
            } else {
                BiometricMock()
            }
        }

        fun cipherCrypter(cipherCrypter: CipherCrypter?): Builder {
            this.cipherCrypter = cipherCrypter
            return this
        }

        fun cipherFactory(cipherFactory: CipherFactory?): Builder {
            this.cipherFactory = cipherFactory
            return this
        }

        fun macCrypter(macCrypter: MacCrypter?): Builder {
            this.macCrypter = macCrypter
            return this
        }

        fun macFactory(macFactory: MacFactory?): Builder {
            this.macFactory = macFactory
            return this
        }

        fun signatureCrypter(signatureCrypter: SignatureCrypter?): Builder {
            this.signatureCrypter = signatureCrypter
            return this
        }

        fun signatureFactory(signatureFactory: SignatureFactory?): Builder {
            this.signatureFactory = signatureFactory
            return this
        }

        fun logEnabled(logEnabled: Boolean): Builder {
            LogUtils.setEnable(logEnabled)
            return this
        }

        @RequiresApi(Build.VERSION_CODES.M)
        private fun buildBiometric(useEnhanced: Boolean): BiometricCompat {
            if (macCrypter == null && signatureCrypter == null && cipherCrypter == null) {
                cipherCrypter = Base64CipherCrypter()
            }
            if (macFactory == null && signatureFactory == null && cipherFactory == null) {
                cipherFactory = AesCipherFactory(context)
            }
            val asyncFactory = AsyncCryptoObjectFactory(
                CryptoObjectFactory(
                    cipherFactory,
                    macFactory,
                    signatureFactory
                )
            )
            val cryptoProxy = CrypterProxy(
                cipherCrypter,
                macCrypter,
                signatureCrypter
            )

            return if (useEnhanced)
                EnhancedBiometricImpl(
                    context,
                    asyncFactory,
                    cryptoProxy
                )
            else
                SystemBiometricImpl(
                    context,
                    asyncFactory,
                    cryptoProxy
                )
        }

        private fun ensureParamsValid() {
            if (macFactory != null && macCrypter == null
                || macFactory == null && macCrypter != null
            ) {
                throw RuntimeException(
                    "To use CryptoObject with MacObject you must provide both MacFactory and "
                            + "MacCrypter implementation. Use BiometricCompat.Builder#macFactory(MacFactory) and "
                            + "BiometricCompat.Builder#macCrypter(MacCrypter) methods to set values."
                )
            }
            if (signatureFactory != null && signatureCrypter == null
                || signatureFactory == null && signatureCrypter != null
            ) {
                throw RuntimeException(
                    "To use CryptoObject with SignatureObject you must provide both SignatureFactory and "
                            + "SignatureCrypter implementation. Use BiometricCompat.Builder#signatureFactory(SignatureFactory) and "
                            + "BiometricCompat.Builder#signatureCrypter(SignatureCrypter) methods to set values."
                )
            }
        }
    }

    class PromptParams private constructor(
        val dialogOwner: Any,
        val title: String?,
        val subtitle: String?,
        val description: String?,
        val negativeButtonText: String?,
        val positiveButtonText: String?,
        val confirmationRequired: Boolean,
        val deviceCredentialsAllowed: Boolean,
        val useFingerprint: Boolean,
        val reactivateBiometricWhenLockout: Boolean,
        val customFingerprintDialogClass: Class<out BaseFingerprintDialogFragment>?
    ) {

        class Builder {
            /* Dialog dialogOwner can be either Fragment or FragmentActivity */
            private val dialogOwner: Any
            private var title: String? = null
            private var subtitle: String? = null
            private var description: String? = null
            private var negativeButtonText: String? = null
            private var positiveButtonText: String? = null
            private var confirmationRequired = false
            private var deviceCredentialsAllowed = false
            private var useFingerprint = true
            private var reactivateBiometricWhenLockout = false
            private var customFingerprintDialogClass: Class<out BaseFingerprintDialogFragment>? =
                null

            constructor(activity: FragmentActivity) {
                dialogOwner = activity
            }

            constructor(fragment: Fragment) {
                dialogOwner = fragment
            }

            fun build(): PromptParams {
                return PromptParams(
                    dialogOwner,
                    title,
                    subtitle,
                    description,
                    negativeButtonText,
                    positiveButtonText,
                    confirmationRequired,
                    deviceCredentialsAllowed,
                    useFingerprint,
                    reactivateBiometricWhenLockout,
                    customFingerprintDialogClass
                )
            }

            /**
             * @see BiometricPrompt.PromptInfo.Builder.setTitle
             */
            fun title(title: String): Builder {
                this.title = title
                return this
            }

            /**
             * @see BiometricPrompt.PromptInfo.Builder.setSubtitle
             */
            fun subtitle(subtitle: String?): Builder {
                this.subtitle = subtitle
                return this
            }

            /**
             * @see BiometricPrompt.PromptInfo.Builder.setDescription
             */
            fun description(description: String?): Builder {
                this.description = description
                return this
            }

            /**
             * @see BiometricPrompt.PromptInfo.Builder.setNegativeButtonText
             */
            fun negativeButtonText(negativeButtonText: String): Builder {
                this.negativeButtonText = negativeButtonText
                return this
            }

            /**
             * Only valid when using enhanced biometrics.
             *
             * @see androidx.biometric.enhanced.BiometricPrompt.PromptInfo.Builder.setPositiveButtonText
             */
            fun positiveButtonText(positiveButtonText: String): Builder {
                this.positiveButtonText = positiveButtonText
                return this
            }

            /**
             * @see BiometricPrompt.PromptInfo.Builder.setConfirmationRequired
             */
            fun confirmationRequired(confirmationRequired: Boolean): Builder {
                this.confirmationRequired = confirmationRequired
                return this
            }

            /**
             * @see BiometricPrompt.PromptInfo.Builder.setDeviceCredentialAllowed
             */
            fun deviceCredentialsAllowed(deviceCredentialsAllowed: Boolean): Builder {
                this.deviceCredentialsAllowed = deviceCredentialsAllowed
                return this
            }

            /**
             * Only valid when using enhanced biometrics.
             *
             * @see androidx.biometric.enhanced.BiometricPrompt.PromptInfo.Builder.setUseFingerprint
             */
            fun useFingerprint(useFingerprint: Boolean): Builder {
                this.useFingerprint = useFingerprint
                return this
            }

            /**
             * Only valid when using enhanced biometrics.
             *
             * @see androidx.biometric.enhanced.BiometricPrompt.PromptInfo.Builder.setCustomFingerprintDialogClass
             */
            fun customFingerprintDialogClass(clazz: Class<out BaseFingerprintDialogFragment>): Builder {
                this.customFingerprintDialogClass = clazz
                return this
            }

            /**
             * Only valid when using enhanced biometrics.
             *
             * @see androidx.biometric.enhanced.BiometricPrompt.PromptInfo.Builder.setReactivateBiometricWhenLockout
             * @see androidx.biometric.enhanced.BiometricConstants.ERROR_LOCKOUT
             * @see androidx.biometric.enhanced.BiometricConstants.ERROR_LOCKOUT_PERMANENT
             */
            fun reactivateBiometricWhenLockout(reactivateBiometricWhenLockout: Boolean): Builder {
                this.reactivateBiometricWhenLockout = reactivateBiometricWhenLockout
                return this
            }
        }
    }

    /**
     * Result wrapper class containing all the useful information about
     * fingerprint authentication and value for encryption/decryption operations.
     */
    data class Result internal constructor(
        /**
         * @see BiometricCompat.Type
         */
        val type: Type,
        /**
         * @see BiometricCompat.Reason
         */
        val reason: Reason,
        /**
         * Authentication value. If standard [BiometricCompat.authenticate] method is used,
         * returned value is null.
         * <p>
         * IFF [BiometricCompat.encrypt] or [BiometricCompat.decrypt]
         * is used, the value contains encrypted or decrypted String.
         * <p>
         * In all other cases, the value is null.
         */
        val value: String? = null,
        /**
         * System message returned by [BiometricPrompt.AuthenticationCallback].
         * A human-readable error string that can be shown in UI.
         */
        val message: String? = null,
        /**
         *  reason 为 [Reason.AUTHENTICATION_EXCEPTION] 时的值
         */
        val exception: Exception? = null
    )

    /**
     * Describes in detail why [authenticate]｜[encrypt]|[decrypt] is dispatched.
     */
    enum class Reason {
        /**
         * @see BiometricPrompt.ERROR_HW_UNAVAILABLE
         */
        HW_UNAVAILABLE,
        /**
         * @see BiometricPrompt.ERROR_UNABLE_TO_PROCESS
         */
        UNABLE_TO_PROCESS,
        /**
         * @see BiometricPrompt.ERROR_TIMEOUT
         */
        TIMEOUT,
        /**
         * @see BiometricPrompt.ERROR_NO_SPACE
         */
        NO_SPACE,
        /**
         * @see BiometricPrompt.ERROR_CANCELED
         */
        CANCELED,
        /**
         * @see BiometricPrompt.ERROR_LOCKOUT
         */
        LOCKOUT,
        /**
         * @see BiometricPrompt.ERROR_VENDOR
         */
        VENDOR,
        /**
         * @see BiometricPrompt.ERROR_LOCKOUT_PERMANENT
         */
        LOCKOUT_PERMANENT,
        /**
         * @see BiometricPrompt.ERROR_USER_CANCELED
         */
        USER_CANCELED,
        /**
         * @see BiometricPrompt.ERROR_NO_BIOMETRICS
         */
        NO_BIOMETRICS,
        /**
         * @see BiometricPrompt.ERROR_HW_NOT_PRESENT
         */
        HW_NOT_PRESENT,
        /**
         * @see BiometricPrompt.ERROR_NEGATIVE_BUTTON
         */
        NEGATIVE_BUTTON,
        /**
         * @see BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL
         */
        NO_DEVICE_CREDENTIAL,
        /**
         * @see androidx.biometric.enhanced.BiometricPrompt.ERROR_POSITIVE_BUTTON
         */
        POSITIVE_BUTTON,
        /**
         * Dispatched when Fingerprint authentication is initialized correctly and
         * just before actual authentication is started. Can be used to update UI if necessary.
         */
        AUTHENTICATION_START,
        /**
         * @see BiometricPrompt.AuthenticationCallback.onAuthenticationSucceeded
         */
        AUTHENTICATION_SUCCESS,
        /**
         * @see BiometricPrompt.AuthenticationCallback.onAuthenticationFailed
         */
        AUTHENTICATION_FAIL,
        /**
         * Critical error happened and fingerprint authentication is stopped.
         */
        AUTHENTICATION_EXCEPTION,
        /**
         * Unknown reason.
         */
        UNKNOWN
    }

    /**
     * Describes the type of the result received in [authenticate]｜[encrypt]|[decrypt]
     */
    enum class Type {
        /**
         * Fingerprint authentication is successfully finished. [BiometricCompat.Result]
         * will contain value in case of [PromptParams.Builder.decrypt] or
         * [PromptParams.Builder.encrypt] invocation.
         */
        SUCCESS,
        /**
         * Fingerprint authentication is still active. [BiometricCompat.Result] contains
         * additional information about currently active fingerprint authentication.
         */
        INFO,
        /**
         * Fingerprint authentication is unsuccessfully finished. [BiometricCompat.Result]
         * contains the reason why the authentication failed.
         */
        ERROR
    }
}