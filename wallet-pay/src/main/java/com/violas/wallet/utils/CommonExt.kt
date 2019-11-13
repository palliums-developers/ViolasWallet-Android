package com.violas.wallet.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import com.violas.wallet.ContextProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.*

fun Intent.start(context: Context) {
    context.startActivity(this)
}

fun Intent.start(activity: Activity, requestCode: Int) {
    activity.startActivityForResult(this, requestCode)
}

fun Intent.start(fragment: Fragment, requestCode: Int) {
    fragment.startActivityForResult(this, requestCode)
}

open class TextWatcherSimple : TextWatcher {
    override fun afterTextChanged(s: Editable?) {

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }
}

public fun IOScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun isMainThread(): Boolean {
    return Looper.getMainLooper() == Looper.myLooper()
}

fun getActiveNetworkInfo(): NetworkInfo? {
    var cm: ConnectivityManager =
        ContextProvider.getContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return cm.activeNetworkInfo
}

fun isNetworkConnected() = getActiveNetworkInfo()?.isConnected ?: false

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