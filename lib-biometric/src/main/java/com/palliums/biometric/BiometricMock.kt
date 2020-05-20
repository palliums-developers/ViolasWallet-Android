package com.palliums.biometric

/**
 * Created by elephant on 2020/5/20 10:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Legacy implementation for pre-Marshmallow devices.
 */
class BiometricMock : BiometricCompat {

    override fun hasBiometricHardware(): Boolean {
        return false
    }

    override fun hasEnrolledBiometric(): Boolean {
        return false
    }

    override fun canAuthenticate(): Boolean {
        return false
    }

    override fun authenticate(
        params: BiometricCompat.PromptParams,
        callback: BiometricCompat.Callback
    ) {

    }

    override fun encrypt(
        params: BiometricCompat.PromptParams,
        key: String,
        value: String,
        callback: BiometricCompat.Callback
    ) {

    }

    override fun decrypt(
        params: BiometricCompat.PromptParams,
        key: String,
        value: String,
        callback: BiometricCompat.Callback
    ) {

    }

    override fun cancel() {

    }
}