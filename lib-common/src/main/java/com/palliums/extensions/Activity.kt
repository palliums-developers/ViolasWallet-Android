package com.palliums.extensions

import android.app.Activity
import androidx.core.app.ActivityCompat

/**
 * Created by elephant on 2020/6/9 11:02.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun Activity.close() {
    if (!isDestroyed && !isFinishing) {
        ActivityCompat.finishAfterTransition(this)
    }
}
