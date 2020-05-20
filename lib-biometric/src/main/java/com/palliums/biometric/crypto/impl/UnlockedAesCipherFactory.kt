package com.palliums.biometric.crypto.impl

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Created by elephant on 2020/5/19 17:10.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Unlocked Cipher factory which can be reused in the app if needed
 * outside of Biometric flow.
 * <p>
 * Standard use case is PIN/Fingerprint login flow. If user does not want
 * to use fingerprint, you can reuse this cipher to encrypt/decrypt his PIN
 * without the need of user authentication.
 */
@RequiresApi(Build.VERSION_CODES.M)
class UnlockedAesCipherFactory(context: Context) : AesCipherFactory(context) {

    override fun isUserAuthRequired(): Boolean {
        return false
    }
}