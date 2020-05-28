package com.palliums.extensions

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

/**
 * Created by elephant on 2020/5/28 15:41.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun DialogFragment.show(fragmentManager: FragmentManager) {
    show(fragmentManager, this.javaClass.name)
}

fun DialogFragment.close() {
    if (!isDetached && !isRemoving && fragmentManager != null) {
        dismissAllowingStateLoss()
    }
}