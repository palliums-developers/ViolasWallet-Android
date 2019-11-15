package com.palliums.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment

/**
 * Created by elephant on 2019-11-14 11:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun Intent.start(context: Context) {
    context.startActivity(this)
}

fun Intent.start(activity: Activity, requestCode: Int) {
    activity.startActivityForResult(this, requestCode)
}

fun Intent.start(fragment: Fragment, requestCode: Int) {
    fragment.startActivityForResult(this, requestCode)
}