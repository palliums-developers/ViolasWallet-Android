package com.violas.wallet.utils

import android.app.Activity
import android.os.Build
import com.palliums.utils.getString
import com.palliums.utils.sendEmail
import com.violas.wallet.BuildConfig
import com.violas.wallet.R

/**
 * Created by elephant on 2019-12-11 13:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

private const val FEEDBACK_EMAIL_ADDRESS = "violas_blockchain@violas.io"

fun feedbackByEmail(activity: Activity) {
    val stringBuilder = StringBuilder()
    stringBuilder.append("\n").append("\n").append("\n").append("\n")
    stringBuilder.append("\n").append("\n").append("\n").append("\n")
    stringBuilder.append("\n")
        .append("------------------------------------------------------------")
    stringBuilder.append("\n")
        .append("App version: ${BuildConfig.VERSION_NAME}.${BuildConfig.VERSION_CODE}")
    stringBuilder.append("\n")
        .append("Phone model: ${Build.BRAND} ${Build.MODEL}")
    stringBuilder.append("\n")
        .append("Android OS version: ${Build.VERSION.RELEASE}")

    sendEmail(
        activity,
        FEEDBACK_EMAIL_ADDRESS,
        getString(R.string.subject_feedback),
        stringBuilder.toString()
    )
}