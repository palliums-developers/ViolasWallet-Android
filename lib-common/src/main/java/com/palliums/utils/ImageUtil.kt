package com.palliums.utils

import android.graphics.BitmapFactory
import java.io.File
import java.util.*

/**
 * Created by elephant on 2019-12-23 17:06.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun String.isImageName(): Boolean {
    if (isEmpty() || trim().isEmpty()) {
        return false
    }

    val upperCase = toUpperCase(Locale.getDefault())
    return upperCase.endsWith(".JPG")
            || upperCase.endsWith(".JPEG")
            || upperCase.endsWith(".ICO")
            || upperCase.endsWith(".ICON")
            || upperCase.endsWith(".PNG")
            || upperCase.endsWith(".WEBP")
            || upperCase.endsWith(".SVG")
            || upperCase.endsWith(".BMP")
            || upperCase.endsWith(".GIF")
            || upperCase.endsWith(".DIB")
            || upperCase.endsWith(".PCP")
            || upperCase.endsWith(".DIF")
            || upperCase.endsWith(".WMF")
            || upperCase.endsWith(".TIF")
            || upperCase.endsWith(".EPS")
            || upperCase.endsWith(".PSD")
            || upperCase.endsWith(".CDR")
            || upperCase.endsWith(".IFF")
            || upperCase.endsWith(".TGA")
            || upperCase.endsWith(".PCD")
            || upperCase.endsWith(".MPT")
            || upperCase.endsWith(".PNT")
            || upperCase.endsWith(".PICT")
            || upperCase.endsWith(".PICT2")
            || upperCase.endsWith(".MNG")
}

fun File.getImageName(): String? {
    if (name.isImageName()) {
        return name
    }

    return try {
        val options = BitmapFactory.Options()
            .apply {
                inJustDecodeBounds = true
            }
        BitmapFactory.decodeFile(absolutePath, options)
        val mimeType = options.outMimeType
        val typeArr = mimeType.split("/")
        "$name.${typeArr[1]}"
    } catch (e: Exception) {
        null
    }
}