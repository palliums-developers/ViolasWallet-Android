package com.palliums.utils

import android.os.Build
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by elephant on 2019-11-14 11:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun getUniquePseudoID(): String {

    val szDevIDShort = StringBuilder("35")

    szDevIDShort.append(Build.BOARD.length % 10)
    szDevIDShort.append(Build.BRAND.length % 10)
    szDevIDShort.append(Build.MODEL.length % 10)
    szDevIDShort.append(Build.DEVICE.length % 10)
    szDevIDShort.append(Build.PRODUCT.length % 10)
    szDevIDShort.append(Build.MANUFACTURER.length % 10)

    val supportedAbis = Build.SUPPORTED_ABIS
    supportedAbis.forEach {
        szDevIDShort.append(it.length % 10)
    }

    val serial = try {
        Build::class.java.getField("SERIAL").get(null).toString()
    } catch (e: Exception) {
        "serial"
    }

    return UUID(szDevIDShort.toString().hashCode().toLong(), serial.hashCode().toLong()).toString()
}

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