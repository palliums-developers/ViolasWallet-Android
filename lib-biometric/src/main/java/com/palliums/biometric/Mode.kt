package com.palliums.biometric

import androidx.annotation.RestrictTo

/**
 * Created by elephant on 2020/5/19 17:27.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Internal enum used to differentiate Fingerprint authentication modes.
 * Authentication does not have to work with cipher, while both
 * Decryption and Encryption should.
 * <p>
 * Contains cipherMode parameter that is used on Biometric initialization.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal enum class Mode {
    AUTHENTICATION, DECRYPTION, ENCRYPTION
}