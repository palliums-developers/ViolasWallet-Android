package com.palliums.utils

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Created by elephant on 2019-11-14 11:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun CustomMainScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.Main + coroutineExceptionHandler())

fun CustomIOScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler())

fun <T> CoroutineScope.exceptionAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {
    return this.async(SupervisorJob() + context, start, block)
}

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

fun <T> LiveData<T>.getDistinct(
    firstNotify: Boolean = false,
    notifyIfDataNull: Boolean = false
): LiveData<T> {
    val distinctLiveData = MediatorLiveData<T>()
    distinctLiveData.addSource(this, object : Observer<T> {
        private var initialized = false
        private var lastObj: T? = null
        override fun onChanged(obj: T?) {
            if (!initialized) {
                initialized = true
                lastObj = obj
                if (firstNotify && (obj != null || notifyIfDataNull)) {
                    distinctLiveData.postValue(obj)
                }
            } else if (obj != null || (notifyIfDataNull && lastObj != null)) {
                lastObj = obj
                distinctLiveData.postValue(obj)
            }
        }
    })
    return distinctLiveData
}
