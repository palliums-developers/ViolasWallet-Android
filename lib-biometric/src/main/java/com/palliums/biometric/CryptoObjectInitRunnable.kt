package com.palliums.biometric

import android.os.Handler
import android.os.Looper


/**
 * Created by elephant on 2020/5/19 18:15.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Custom runnable that creates CryptoObject.
 * Used for asynchronous creation.
 */
class CryptoObjectInitRunnable(
    private val cryptoObjectFactory: CryptoObjectFactory,
    private val mode: Mode,
    private val key: String,
    private val callback: AsyncCryptoObjectFactory.Callback
) : Runnable {

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    override fun run() {
        val cryptoObject =
            cryptoObjectFactory.createCryptoObject(key, mode)

        if (!callback.isCanceled()) {
            mainHandler.post {
                callback.onCryptoObjectCreated(cryptoObject)
            }
        }
    }
}