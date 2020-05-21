package com.palliums.biometric.util

import android.util.Log
import androidx.annotation.RestrictTo
import java.util.*
import com.palliums.biometric.BiometricCompat

/**
 * Created by elephant on 2020/5/20 14:16.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Logging utility functions. Logger can be turned on/off from [BiometricCompat]
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class LogUtils {

    companion object {
        private const val TAG = "BiometricCompat"
        private var enable = false

        fun log(message: String, vararg args: Any) {
            if (enable) {
                Log.i(TAG, String.format(Locale.US, message, args))
            }
        }

        fun log(t: Throwable) {
            if (enable) {
                Log.e(TAG, null, t)
            }
        }

        fun setEnable(enable: Boolean) {
            this.enable = enable
        }
    }
}