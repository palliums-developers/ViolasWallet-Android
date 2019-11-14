package com.palliums.utils

import android.os.Looper

/**
 * Created by elephant on 2019-11-14 11:12.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun isMainThread(): Boolean {
    return Looper.getMainLooper().thread === Thread.currentThread()
}