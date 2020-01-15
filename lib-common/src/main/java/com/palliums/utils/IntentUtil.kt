package com.palliums.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
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

fun openEmailClient(
    activity: Activity,
    receiver: String = "",
    subject: String = "",
    text: String = "",
    handleError: Boolean = true
) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:$receiver")
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, text)

        val resolveInfos =
            activity.packageManager.queryIntentActivities(intent, 0)
        if (resolveInfos.isEmpty()) {
            throw Resources.NotFoundException()
        }

        /*if (resolveInfos.size == 1) {
            activity.startActivity(intent)
        } else {
            activity.startActivity(
                Intent.createChooser(
                    intent,
                    getString(R.string.title_select_mail_client)
                )
            )
        }*/

        activity.startActivity(intent)
    } catch (e: Exception) {
        if (handleError) {
            if (activity is BaseActivity) {
                activity.showToast(
                    if (e is Resources.NotFoundException)
                        R.string.tips_email_client_not_found
                    else
                        R.string.tips_open_email_client_failure
                )
            } else {
                // ignore
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

        val resolveInfos =
            activity.packageManager.queryIntentActivities(intent, 0)
        /*val browserIntentMap = mutableMapOf<String, Intent>()
        var packageName: String
        resolveInfos.forEach {
            packageName = it.activityInfo.packageName
            if (!browserIntentMap.containsKey(packageName)) {
                if (isSystemApp(packageName)
                    || packageName == "com.android.chrome"
                    || packageName == "org.mozilla.firefox"
                    || packageName == "com.tencent.mtt"
                    || packageName == "com.UCMobile"
                    || packageName == "com.baidu.browser.apps"
                    || packageName == "com.qihoo.browser"
                    || packageName == "com.ijinshan.browser_fast"
                    || packageName == "sogou.mobile.explorer"
                    || packageName.contains("browser", true)
                    || packageName.contains("explorer", true)
                ) {
                    activity.packageManager.getLaunchIntentForPackage(packageName)?.run {
                        action = Intent.ACTION_VIEW
                        data = uri
                        browserIntentMap[packageName] = this
                    }
                }
            }
        }

        if (browserIntentMap.isEmpty()) {
            throw Resources.NotFoundException()
        }

        if (browserIntentMap.size == 1) {
            activity.startActivity(intent)
        } else {
            val browserIntents = browserIntentMap.values.toMutableList()
            val chooserIntent = Intent.createChooser(
                browserIntents.removeAt(0),
                getString(R.string.title_select_browser)
            ).apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, browserIntents.toTypedArray())
            }
            activity.startActivity(chooserIntent)
        }*/

        if (resolveInfos.isEmpty()) {
            throw Resources.NotFoundException()
        }

        /*if (resolveInfos.size == 1) {
            activity.startActivity(intent)
        } else {
            activity.startActivity(
                Intent.createChooser(
                    intent,
                    getString(R.string.title_select_browser)
                )
            )
        }*/

        activity.startActivity(intent)

        true
    } catch (ignore: Exception) {
        false
    }
}