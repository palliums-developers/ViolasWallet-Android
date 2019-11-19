package com.violas.wallet.ui.web

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.start
import com.violas.wallet.base.BaseWebActivity
import com.violas.wallet.common.EXTRA_KEY_TITLE
import com.violas.wallet.common.EXTRA_KEY_URL

/**
 * Created by elephant on 2019-10-31 18:09.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Web通用页面
 */
class WebCommonActivity : BaseWebActivity() {

    companion object {
        @JvmStatic
        fun start(context: Context, url: String, title: String? = null) {
            Intent(context, WebCommonActivity::class.java).also {
                it.putExtra(EXTRA_KEY_URL, url)
                if (title != null) {
                    it.putExtra(EXTRA_KEY_TITLE, title)
                }
            }.start(context)
        }
    }

    private lateinit var mUrl: String
    private var mTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        var url: String? = null
        var title: String? = null
        savedInstanceState?.let {
            url = it.getString(EXTRA_KEY_URL, null)
            title = it.getString(EXTRA_KEY_TITLE, null)
        }

        if (url.isNullOrEmpty()) {
            intent?.let {
                url = it.getStringExtra(EXTRA_KEY_URL)
                title = it.getStringExtra(EXTRA_KEY_TITLE)
            }

            if (url.isNullOrEmpty()) {
                finish()
                return
            }
        }

        mUrl = url!!
        mTitle = title

        super.onCreate(savedInstanceState)
    }

    override fun onNewIntent(intent: Intent?) {
        val url = intent?.getStringExtra(EXTRA_KEY_URL)
        val title = intent?.getStringExtra(EXTRA_KEY_TITLE)
        if (!url.isNullOrEmpty()) {
            mUrl = url
            mTitle = title

            super.onNewIntent(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(EXTRA_KEY_URL, mUrl)
        mTitle?.let { outState.putString(EXTRA_KEY_TITLE, it) }
    }

    override fun getUrl(): String {
        return mUrl
    }

    override fun getFixedTitle(): String? {
        return mTitle
    }
}