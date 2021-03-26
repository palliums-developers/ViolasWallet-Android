package com.palliums.base

import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Created by elephant on 2019-11-05 18:30.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface ViewController {

    fun showProgress(@StringRes resId: Int)

    fun showProgress(msg: String? = null)

    fun dismissProgress()

    fun showToast(@StringRes msgId: Int, duration: Int = Toast.LENGTH_LONG)

    fun showToast(msg: String, duration: Int = Toast.LENGTH_LONG)
}