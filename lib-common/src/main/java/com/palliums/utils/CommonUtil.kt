package com.palliums.utils

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Created by elephant on 2019-11-14 11:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun CustomMainScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.Main + coroutineExceptionHandler())

fun coroutineExceptionHandler() = CoroutineExceptionHandler { _, exception ->
    exception.printStackTrace()
}

class CommonViewHolder(view: View) : RecyclerView.ViewHolder(view)

fun <T> List<T>.toMap(key: (T) -> String): Map<String, T> {
    val map = HashMap<String, T>(this.size)
    this.iterator().forEach {
        map[key.invoke(it)] = it
    }
    return map
}

fun <T> List<T>.toMutableMap(key: (T) -> String): MutableMap<String, T> {
    val map = mutableMapOf<String, T>()
    this.iterator().forEach {
        map[key.invoke(it)] = it
    }
    return map
}

public inline fun String.toBigDecimal(): java.math.BigDecimal {
    return if (this.isEmpty()) {
        java.math.BigDecimal("0")
    } else {
        java.math.BigDecimal(this)
    }
}
