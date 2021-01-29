package com.palliums.utils

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Base64
import com.palliums.content.ContextProvider
import com.palliums.extensions.logError
import java.io.File
import java.io.OutputStream
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

fun String.toBitmap(): Bitmap? {
    return try {
        val bitmapArray = Base64.decode(this, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.size)
    } catch (e: Exception) {
        logError(e) { "String to Bitmap failed" }
        null
    }
}

fun Bitmap.saveIntoSystemAlbum(
    dirName: String,
    context: Context = ContextProvider.getContext()
): Boolean {
    var outputStream: OutputStream? = null
    try {
        val picDir = context.getExternalFilesDir(dirName) ?: return false
        if (!picDir.exists()) {
            picDir.mkdirs()
        }

        val curTime = System.currentTimeMillis()
        val picName = "$curTime.png"
        val picPath = File(picDir, picName).absolutePath
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.ImageColumns.DATA, picPath)
        contentValues.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, picName)
        contentValues.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/png")
        contentValues.put(MediaStore.Images.ImageColumns.DATE_ADDED, curTime / 1000)
        contentValues.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, curTime / 1000)
        contentValues.put(MediaStore.Images.ImageColumns.SIZE, this.byteCount)

        val contentResolver = context.contentResolver
        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return false

        outputStream = contentResolver.openOutputStream(uri)
        this.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return true
    } catch (e: Exception) {
        logError(e) { "Failed to save Bitmap to system album" }
        return false
    } finally {
        try {
            outputStream?.let {
                it.flush()
                it.close()
            }
        } catch (ignore: Exception) {
        }
        try {
            if (!this.isRecycled) {
                this.recycle()
            }
        } catch (e: Exception) {
        }
    }
}
