package com.violas.wallet.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.widget.Toast
import com.violas.wallet.R

class ClipboardUtils {
    companion object {
        @JvmStatic
        fun copy(context: Context, message: String) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // 将文本内容放到系统剪贴板里。
            cm.setPrimaryClip(ClipData.newPlainText("bitcoin_Address", message))
            Toast.makeText(context, R.string.copy_success, Toast.LENGTH_SHORT).show()
        }

        @JvmStatic
        fun getClipData(context: Context): String {
            val cm = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val data = cm.primaryClip
            val item = data?.getItemAt(0)
            val content = item?.text?.toString()
            return content ?: ""
        }

    }
}

