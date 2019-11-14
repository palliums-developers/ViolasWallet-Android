package com.palliums.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.palliums.content.ContextProvider

/**
 * Created by elephant on 2019-11-14 11:09.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun getActiveNetworkInfo(): NetworkInfo? {
    var cm: ConnectivityManager =
        ContextProvider.getContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return cm.activeNetworkInfo
}

fun isNetworkConnected() = getActiveNetworkInfo()?.isConnected ?: false