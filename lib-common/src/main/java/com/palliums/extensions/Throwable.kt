package com.palliums.extensions

import com.palliums.BuildConfig
import com.palliums.R
import com.palliums.exceptions.BaseException
import com.palliums.exceptions.RequestException
import com.palliums.utils.getString
import com.palliums.utils.isNetworkConnected

/**
 * Created by elephant on 2020/5/14 11:35.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun Throwable.isNoNetwork(): Boolean {
    return (this is RequestException && this.errorCode == RequestException.ERROR_CODE_NO_NETWORK)
            || !isNetworkConnected()
}

fun Throwable.isActiveCancellation(): Boolean {
    return (this is RequestException && this.errorCode == RequestException.ERROR_CODE_ACTIVE_CANCELLATION)
            || this.javaClass.name == "kotlinx.coroutines.JobCancellationException"
}

fun Throwable.getShowErrorMessage(loadAction: Boolean): String {
    if (this is BaseException) {
        return getErrorMessage(loadAction)
    }

    if (loadAction) {
        return if (BuildConfig.DEBUG)
            "${getString(R.string.common_load_fail)}\n${toString()}"
        else
            getString(R.string.common_load_fail)
    }

    return if (BuildConfig.DEBUG)
        "${getString(R.string.common_operation_fail)}\n${toString()}"
    else
        getString(R.string.common_operation_fail)
}