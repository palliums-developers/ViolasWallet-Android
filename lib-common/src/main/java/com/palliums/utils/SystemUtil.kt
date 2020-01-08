package com.palliums.utils

import android.os.Build
import java.util.*

/**
 * Created by elephant on 2020-01-08 13:38.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun getHttpUserAgent(): String {
    val userAgent = try {
        System.getProperty("http.agent")
    } catch (ignore: Exception) {
        null
    }

    if (userAgent.isNullOrEmpty()) {
        return makeUserAgent()
    }

    return try {
        val stringBuilder = StringBuilder()
        userAgent.forEach {
            if (it <= '\u001f' || it >= '\u007f') {
                stringBuilder.append(String.format("\\u%04x", it.toInt()))
            } else {
                stringBuilder.append(it)
            }
        }
        stringBuilder.toString()
    } catch (ignore: Exception) {
        makeUserAgent()
    }
}

fun makeUserAgent(): String {
    return "${getJavaVM()} (${getBaseOS()}; U; Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID})"
}

fun getJavaVM(): String {
    try {
        val vmName = System.getProperty("java.vm.name")
        val vmVersion = System.getProperty("java.vm.version")
        if (!vmName.isNullOrEmpty()) {
            return "$vmName${if (vmVersion.isNullOrEmpty()) "" else "/${vmVersion}"}"
        }
    } catch (ignore: Throwable) {
    }

    return "VM unknown"
}

fun getBaseOS(): String {
    try {
        val osName = System.getProperty("os.name")
        if (!osName.isNullOrEmpty()) {
            return osName
        }
    } catch (ignore: Throwable) {
    }

    return "Linux"
}

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