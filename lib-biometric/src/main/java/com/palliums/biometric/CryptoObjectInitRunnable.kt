package com.palliums.biometric

import android.os.Handler
import android.os.Looper
import androidx.annotation.RestrictTo


/**
 * Created by elephant on 2020/5/19 18:15.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Custom runnable that creates CryptoObject.
 * Used for asynchronous creation.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class CryptoObjectInitRunnable(
    private val cryptoObjectFactory: CryptoObjectFactory,
    private val mode: Mode,
    private val key: String,
    private val callback: AsyncCryptoObjectFactory.Callback
) : Runnable {

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    override fun run() {
        val cryptoObject =
            cryptoObjectFactory.createCryptoObject(mode, key)

        if (!callback.isCanceled()) {
            mainHandler.post {
                callback.onCryptoObjectCreated(cryptoObject)
            }
        }
    }
}