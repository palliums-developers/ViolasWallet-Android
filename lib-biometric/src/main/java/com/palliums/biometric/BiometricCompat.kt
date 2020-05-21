package com.palliums.biometric

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.palliums.biometric.crypto.*
import com.palliums.biometric.crypto.impl.AesCipherFactory
import com.palliums.biometric.crypto.impl.Base64CipherCrypter
import com.palliums.biometric.util.LogUtils

/**
 * Created by elephant on 2020/5/19 17:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface BiometricCompat {

    /**
     * Returns true if user has biometric hardware, false otherwise.
     */
    fun hasBiometricHardware(): Boolean

    /**
     * Returns true if user has enrolled biometric, false otherwise.
     */
    fun hasEnrolledBiometric(): Boolean

    /**
     * @see [BiometricManager.canAuthenticate]
     */
    fun canAuthenticate(): Boolean

    /**
     * Authenticate user via Fingerprint.
     * <p>
     * Example - Process payment after successful fingerprint authentication.
     *
     * @see PromptParams
     */
    fun authenticate(
        params: PromptParams,
        callback: Callback
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
     * @param callback callback
     * @see BiometricCompat.Callback
     */
    fun encrypt(
        params: PromptParams,
        key: String,
        value: String,
        callback: Callback
    )

    /**
     * Authenticate user via Fingerprint. If user is successfully authenticated,
     * [CrypterProxy] implementation is used to automatically decrypt given value.
     *
     *
     * Should be used together with [BiometricCompat.encrypt] to decrypt saved data.
     *
     * @param key   unique key identifier, used to load Cipher IV internally
     * @param value String value which will be decrypted if user successfully authenticates
     */
    fun decrypt(
        params: PromptParams,
        key: String,
        value: String,
        callback: Callback
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
        private val mode = Mode.AUTHENTICATION
        private val key: String? = null
        private val value: String? = null

        fun build(useCustom: Boolean = true): BiometricCompat {
            ensureParamsValid()
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                buildBiometric(useCustom)
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

        fun logEnabled(logEnabled: Boolean): Builder {
            LogUtils.setEnable(logEnabled)
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

        @RequiresApi(Build.VERSION_CODES.M)
        private fun buildBiometric(useCustom: Boolean): BiometricCompat {
            if (macCrypter == null && signatureCrypter == null && cipherCrypter == null) {
                cipherCrypter = Base64CipherCrypter()
            }
            if (macFactory == null && signatureFactory == null && cipherFactory == null) {
                cipherFactory = AesCipherFactory(context)
            }
            val asyncFactory =
                AsyncCryptoObjectFactory(
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

            return if (useCustom)
                CustomBiometricImpl(
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
        val confirmationRequired: Boolean,
        val deviceCredentialsAllowed: Boolean
    ) {

        class Builder {
            /* Dialog dialogOwner can be either Fragment or FragmentActivity */
            private val dialogOwner: Any
            private var title: String? = null
            private var subtitle: String? = null
            private var description: String? = null
            private var negativeButtonText: String? = null
            private var confirmationRequired = false
            private var deviceCredentialsAllowed = false
            private val mode = Mode.AUTHENTICATION

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
                    confirmationRequired,
                    deviceCredentialsAllowed
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
        }
    }

    /**
     * Callback used to receive BiometricCompat results.
     */
    interface Callback {
        /**
         * Returns fingerprint result and will be invoked multiple times during
         * fingerprint authentication as not all fingerprint results complete
         * the authentication.
         *
         *
         * Result callback invoked for every fingerprint result (success, error or info).
         * It can be invoked multiple times during single fingerprint authentication.
         *
         * @param result contains fingerprint result information
         * @see BiometricCompat.Result
         */
        fun onResult(result: Result)

        /**
         * Critical error happened and fingerprint authentication is stopped.
         */
        fun onError(e: Exception)
    }

    /**
     * Result wrapper class containing all the useful information about
     * fingerprint authentication and value for encryption/decryption operations.
     */
    class Result @JvmOverloads internal constructor(
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
        val message: String? = null
    )

    /**
     * Describes in detail why [Callback.onResult] is dispatched.
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
         * Unknown reason.
         */
        UNKNOWN
    }

    /**
     * Describes the type of the result received in [Callback.onResult]
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