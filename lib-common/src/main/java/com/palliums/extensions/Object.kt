package com.palliums.extensions

import android.util.Log
import com.palliums.BuildConfig

/**
 * Created by elephant on 2020/7/23 14:24.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

inline fun Any.lazyLogVerbose(
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.v(this.javaClass.canonicalName, lazyMsg.invoke())
}

inline fun Any.lazyLogVerbose(
    tr: Throwable,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.v(this.javaClass.canonicalName, lazyMsg.invoke(), tr)
}

inline fun Any.lazyLogDebug(
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.d(this.javaClass.canonicalName, lazyMsg.invoke())
}

inline fun Any.lazyLogDebug(
    tr: Throwable,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.d(this.javaClass.canonicalName, lazyMsg.invoke(), tr)
}

inline fun Any.lazyLogInfo(
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.i(this.javaClass.canonicalName, lazyMsg.invoke())
}

inline fun Any.lazyLogInfo(
    tr: Throwable,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.i(this.javaClass.canonicalName, lazyMsg.invoke(), tr)
}

inline fun Any.lazyLogWarn(
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.w(this.javaClass.canonicalName, lazyMsg.invoke())
}

inline fun Any.lazyLogWarn(
    tr: Throwable,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.w(this.javaClass.canonicalName, lazyMsg.invoke(), tr)
}

inline fun Any.lazyLogError(
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.e(this.javaClass.canonicalName, lazyMsg.invoke())
}

inline fun Any.lazyLogError(
    tr: Throwable,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.e(this.javaClass.canonicalName, lazyMsg.invoke(), tr)
}