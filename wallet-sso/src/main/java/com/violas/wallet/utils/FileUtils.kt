package com.violas.wallet.utils

import android.content.Context
import android.net.Uri
import com.palliums.content.ContextProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

suspend fun getFilePathFromContentUri(
    inputUri: Uri,
    context: Context = ContextProvider.getContext()
): File = withContext(Dispatchers.IO) {
    val file = File("${context.externalCacheDir?.absoluteFile}${File.separator}${inputUri.path}")
    val dir = file.parentFile
    if (dir?.exists() == false) {
        dir.mkdirs()
    }
    if (!file.exists()) {
        file.createNewFile()
    }
    val outputStream = file.outputStream()
    copyStream(context.contentResolver.openInputStream(inputUri)!!, outputStream)
    file
}

fun copyStream(`in`: InputStream, output: OutputStream) {
    try {
        val buff = ByteArray(4 * 1024)
        var read: Int
        `in`.use { input ->
            output.use { output ->
                while ({ read = input.read(buff);read }() != -1) {
                    output.write(buff)
                }
            }
        }
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}