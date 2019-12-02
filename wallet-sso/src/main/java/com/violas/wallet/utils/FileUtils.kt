package com.violas.wallet.utils

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore

fun getFilePathFromContentUri(
    selectedVideoUri: Uri,
    contentResolver: ContentResolver
): String {
    var filePath = ""
    val filePathColumn = arrayOf(MediaStore.MediaColumns.DATA)

    val cursor = contentResolver.query(selectedVideoUri, filePathColumn, null, null, null)
    // 也可用下面的方法拿到cursor
    // Cursor cursor = this.context.managedQuery(selectedVideoUri,
    // filePathColumn, null, null, null);

    //        cursor.moveToFirst();
    //
    //        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
    //        filePath = cursor.getString(columnIndex);
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            val id = cursor.getColumnIndex(filePathColumn[0])
            if (id > -1)
                filePath = cursor.getString(id)
        }
        cursor.close()
    }

    return filePath
}