package com.palliums.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.palliums.content.ContextProvider
import java.io.File

/**
 * Created by elephant on 2019-12-04 10:06.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun getFilePathByUri(uri: Uri, context: Context = ContextProvider.getContext()): String? {

    var filePath: String? = null

    val scheme = uri.scheme
    if (ContentResolver.SCHEME_FILE.equals(scheme, ignoreCase = true)) {
        filePath = uri.path

    } else if (ContentResolver.SCHEME_CONTENT.equals(scheme, ignoreCase = true)) {
        /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return getDataColumn(context, uri, null, null);
        }*/
        if (DocumentsContract.isDocumentUri(context, uri)) {

            val docId = DocumentsContract.getDocumentId(uri)

            if (isMediaDocument(uri)) {
                // MediaProvider
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                when (type) {
                    "image" -> {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    "video" -> {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }
                    "audio" -> {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                }

                val selection = MediaStore.MediaColumns._ID + "=?"
                val selectionArgs = arrayOf(split[1])

                contentUri?.let {
                    filePath = getDataColumn(it, selection, selectionArgs, context)
                }

            } else if (isDownloadsDocument(uri)) {
                // DownloadsProvider
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    java.lang.Long.valueOf(docId)
                )
                filePath = getDataColumn(contentUri, null, null, context)

            } else if (isExternalStorageDocument(uri)) {
                // ExternalStorageProvider
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    val externalStoragePath = Environment.getExternalStorageDirectory().absolutePath
                    filePath = "$externalStoragePath${File.separator}${split[1]}"
                }
            }
        } else {
            filePath = getDataColumn(uri, null, null, context)

            // https://blog.csdn.net/Jersey_me/article/details/79269382
            //TODO fix java.lang.IllegalArgumentException: column '_data' does not exist
            if (filePath == null && isMyExternalStorageStandardDir(uri, context)) {
                try {
                    val path = uri.toString()
                    val separator = File.separator
                    val packageName = context.packageName
                    val externalStoragePath = Environment.getExternalStorageDirectory().absolutePath

                    var dir = "${separator}Android${separator}data$separator$packageName"
                    var startIndex = path.indexOf(dir)

                    if (startIndex > 0 || startIndex < path.length - 1) {
                        filePath = "$externalStoragePath${path.substring(startIndex)}"

                    } else {
                        dir = "${separator}Android${separator}obb$separator$packageName"
                        startIndex = path.indexOf(dir)

                        if (startIndex > 0 || startIndex < path.length - 1) {
                            filePath = "$externalStoragePath${path.substring(startIndex)}"
                        }
                    }
                } catch (e: Exception) {
                    //ignore
                }
            }
        }
    }

    return filePath
}

fun getDataColumn(
    uri: Uri,
    selection: String?,
    selectionArgs: Array<String>?,
    context: Context = ContextProvider.getContext()
): String? {

    var cursor: Cursor? = null

    return try {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndexOrThrow(projection[0])
            cursor.getString(index)
        } else {
            null
        }
    } catch (throwable: Throwable) {
        null
    } finally {
        try {
            cursor?.close()
        } catch (e: Exception) {
            // ignore
        }
    }
}

fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
}

fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
}

fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
}

/**
 * 是否我的外部存储标准目录 .../Android/data/packageName/...
 * 例如：
 * content://com.sec.android.app.myfiles.FileProvider/device_storage/Android/data/com.taiyiyun.openim.android.client/cache/taiyiyun/userFileAAAAAAAAAADuxet_e2aHJyy58EO4mMoM/cache/shareIn/15367367947993375775912206698629.jpg
 * content://com.sec.android.app.myfiles.FileProvider/device_storage/Android/obb/com.ustwo.monumentvalley/main.2000518026.com.ustwo.monumentvalley.obb
 * content://com.huawei.hidisk.fileprovider/root/storage/emulated/0/Android/data/com.taiyiyun.openim.android.client/cache/taiyiyun/userFileAAAAAAAAAADuxet_e2aHJyy58EO4mMoM/cache/shareIn/1536737941897223482316.jpg
 *
 * @param context
 * @param uri
 * @return
 */
fun isMyExternalStorageStandardDir(
    uri: Uri,
    context: Context = ContextProvider.getContext()
): Boolean {

    val separator = File.separator
    val packageName = context.packageName
    val path = uri.toString()
    return path.contains("Android${separator}data$separator$packageName")
            || path.contains("Android${separator}obb$separator$packageName")
}