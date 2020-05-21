package com.palliums.biometric

import androidx.annotation.RestrictTo
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.Mac

/**
 * Created by elephant on 2020/5/21 17:02.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * A wrapper class for the crypto objects supported by BiometricPrompt. Currently the
 * framework supports [Signature], [Cipher], and [Mac] objects.
 *
 * @see androidx.biometric.BiometricPrompt.CryptoObject
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class CryptoObject {
    /**
     * Get [Signature] object.
     *
     * @return [Signature] object or null if this doesn't contain one.
     */
    val signature: Signature?
    /**
     * Get [Cipher] object.
     *
     * @return [Cipher] object or null if this doesn't contain one.
     */
    val cipher: Cipher?
    /**
     * Get [Mac] object.
     *
     * @return [Mac] object or null if this doesn't contain one.
     */
    val mac: Mac?

    constructor(signature: Signature) {
        this.signature = signature
        cipher = null
        mac = null
    }

    constructor(cipher: Cipher) {
        this.cipher = cipher
        signature = null
        mac = null
    }

    constructor(mac: Mac) {
        this.mac = mac
        cipher = null
        signature = null
    }
}