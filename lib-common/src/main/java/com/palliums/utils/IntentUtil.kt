package com.palliums.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import com.palliums.R
import com.palliums.base.BaseActivity

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

fun sendEmail(
    activity: Activity,
    receiver: String,
    subject: String,
    text: String,
    handleError: Boolean = true
) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:$receiver")
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        activity.startActivity(
            Intent.createChooser(
                intent,
                getString(R.string.title_select_mail_app)
            )
        )
    } catch (e: Exception) {
        if (handleError) {
            if (activity is BaseActivity) {
                activity.showToast(R.string.tips_open_email_failure)
            }
        } else {
            throw e
        }
    }
}

fun openBrowser(activity: Activity, url: String): Boolean {
    return try {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        activity.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}