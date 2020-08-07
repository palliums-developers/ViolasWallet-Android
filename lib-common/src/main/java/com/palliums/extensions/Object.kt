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
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.v(
        tag ?: this.javaClass.canonicalName,
        "[${getThreadName()}] ${lazyMsg.invoke()}"
    )
}

inline fun Any.lazyLogVerbose(
    tr: Throwable,
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.v(
        tag ?: this.javaClass.canonicalName,
        "[${getThreadName()}] ${lazyMsg.invoke()}",
        tr
    )
}

inline fun Any.lazyLogDebug(
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.d(
        tag ?: this.javaClass.canonicalName,
        "[${getThreadName()}] ${lazyMsg.invoke()}"
    )
}

inline fun Any.lazyLogDebug(
    tr: Throwable,
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.d(
        tag ?: this.javaClass.canonicalName,
        "[${getThreadName()}] ${lazyMsg.invoke()}",
        tr
    )
}

inline fun Any.lazyLogInfo(
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.i(
        tag ?: this.javaClass.canonicalName,
        "[${getThreadName()}] ${lazyMsg.invoke()}"
    )
}

inline fun Any.lazyLogInfo(
    tr: Throwable,
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.i(
        tag ?: this.javaClass.canonicalName,
        "[${getThreadName()}] ${lazyMsg.invoke()}",
        tr
    )
}

inline fun Any.lazyLogWarn(
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.w(
        tag ?: this.javaClass.canonicalName,
        "[${getThreadName()}] ${lazyMsg.invoke()}"
    )
}

inline fun Any.lazyLogWarn(
    tr: Throwable,
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.w(
        tag ?: this.javaClass.canonicalName,
        "[${getThreadName()}] ${lazyMsg.invoke()}",
        tr
    )
}

inline fun Any.lazyLogError(
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.e(
        tag ?: this.javaClass.canonicalName,
        "[${getThreadName()}] ${lazyMsg.invoke()}"
    )
}

inline fun Any.lazyLogError(
    tr: Throwable,
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.e(
        tag ?: this.javaClass.canonicalName,
        "[${getThreadName()}] ${lazyMsg.invoke()}",
        tr
    )
}

fun getThreadName(): String {
    val name = Thread.currentThread().name
    return if (name.isNullOrBlank())
        Thread.currentThread().id.toString()
    else
        name
}