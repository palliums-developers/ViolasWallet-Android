package com.palliums.biometric

import androidx.annotation.RestrictTo
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Created by elephant on 2020/5/19 17:44.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Creates CryptoObject asynchronously.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class AsyncCryptoObjectFactory(
    private val cryptoObjectFactory: CryptoObjectFactory
) {

    private val executor by lazy { Executors.newSingleThreadExecutor() }
    private var task: Future<out Any>? = null

    fun createCryptoObject(mode: Mode, key: String, callback: Callback) {
        if (task != null && !task!!.isDone) {
            task!!.cancel(true)
        }

        task = executor.submit(
            CryptoObjectInitRunnable(cryptoObjectFactory, mode, key, callback)
        )
    }

    /**
     * Internal callback used to receive created [CryptoObject]
     */
    abstract class Callback {

        private var canceled = false

        fun cancel() {
            canceled = true
        }

        fun isCanceled(): Boolean {
            return canceled
        }

        abstract fun onCryptoObjectCreated(cryptoObject: CryptoObject?)
    }
}