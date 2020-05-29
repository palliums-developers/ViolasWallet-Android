package com.palliums.biometric

import androidx.annotation.RestrictTo
import androidx.biometric.BiometricManager
import com.palliums.biometric.exceptions.MissingHardwareException

/**
 * Created by elephant on 2020/5/20 10:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Legacy implementation for pre-Marshmallow devices.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class BiometricMock : BiometricCompat {

    override fun canAuthenticate(useFingerprint: Boolean): Int {
        return BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
    }

    override fun authenticate(
        params: BiometricCompat.PromptParams,
        callback: (BiometricCompat.Result) -> Unit
    ) {
        callback.invoke(
            BiometricCompat.Result(
                BiometricCompat.Type.ERROR,
                BiometricCompat.Reason.AUTHENTICATION_EXCEPTION,
                exception = MissingHardwareException()
            )
        )
    }

    override fun encrypt(
        params: BiometricCompat.PromptParams,
        key: String,
        value: String,
        callback: (BiometricCompat.Result) -> Unit
    ) {
        callback.invoke(
            BiometricCompat.Result(
                BiometricCompat.Type.ERROR,
                BiometricCompat.Reason.AUTHENTICATION_EXCEPTION,
                exception = MissingHardwareException()
            )
        )
    }

    override fun decrypt(
        params: BiometricCompat.PromptParams,
        key: String,
        value: String,
        callback: (BiometricCompat.Result) -> Unit
    ) {
        callback.invoke(
            BiometricCompat.Result(
                BiometricCompat.Type.ERROR,
                BiometricCompat.Reason.AUTHENTICATION_EXCEPTION,
                exception = MissingHardwareException()
            )
        )
    }

    override fun cancel() {

    }
}