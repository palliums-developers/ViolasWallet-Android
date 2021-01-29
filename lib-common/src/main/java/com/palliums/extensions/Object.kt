package com.palliums.extensions

import android.util.Log
import com.palliums.BuildConfig

/**
 * Created by elephant on 2020/7/23 14:24.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

inline fun Any.logVerbose(
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.v(
        tag ?: this.javaClass.simpleName,
        "[${getThreadName()}] ${lazyMsg.invoke()}"
    )
}

inline fun Any.logVerbose(
    tr: Throwable,
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.v(
        tag ?: this.javaClass.simpleName,
        "[${getThreadName()}] ${lazyMsg.invoke()}",
        tr
    )
}

inline fun Any.logDebug(
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.d(
        tag ?: this.javaClass.simpleName,
        "[${getThreadName()}] ${lazyMsg.invoke()}"
    )
}

inline fun Any.logDebug(
    tr: Throwable,
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.d(
        tag ?: this.javaClass.simpleName,
        "[${getThreadName()}] ${lazyMsg.invoke()}",
        tr
    )
}

inline fun Any.logInfo(
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.i(
        tag ?: this.javaClass.simpleName,
        "[${getThreadName()}] ${lazyMsg.invoke()}"
    )
}

inline fun Any.logInfo(
    tr: Throwable,
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.i(
        tag ?: this.javaClass.simpleName,
        "[${getThreadName()}] ${lazyMsg.invoke()}",
        tr
    )
}

inline fun Any.logWarn(
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.w(
        tag ?: this.javaClass.simpleName,
        "[${getThreadName()}] ${lazyMsg.invoke()}"
    )
}

inline fun Any.logWarn(
    tr: Throwable,
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.w(
        tag ?: this.javaClass.simpleName,
        "[${getThreadName()}] ${lazyMsg.invoke()}",
        tr
    )
}

inline fun Any.logError(
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.e(
        tag ?: this.javaClass.simpleName,
        "[${getThreadName()}] ${lazyMsg.invoke()}"
    )
}

inline fun Any.logError(
    tr: Throwable,
    tag: String? = null,
    lazyMsg: () -> String
) {
    if (!BuildConfig.DEBUG) return
    Log.e(
        tag ?: this.javaClass.simpleName,
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