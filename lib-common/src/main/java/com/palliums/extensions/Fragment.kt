package com.palliums.extensions

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Created by elephant on 2020/5/28 15:41.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun Fragment.showToast(
    @StringRes msgId: Int,
    duration: Int = Toast.LENGTH_SHORT
) {
    showToast(getString(msgId), duration)
}

fun Fragment.showToast(
    msg: String,
    duration: Int = Toast.LENGTH_SHORT
) {
    context?.let {
        Toast.makeText(it, msg, duration).show()
    }
}

fun DialogFragment.show(fragmentManager: FragmentManager) {
    show(fragmentManager, this.javaClass.name)
}

fun DialogFragment.close() {
    if (!isDetached && !isRemoving && fragmentManager != null) {
        dismissAllowingStateLoss()
    }
}