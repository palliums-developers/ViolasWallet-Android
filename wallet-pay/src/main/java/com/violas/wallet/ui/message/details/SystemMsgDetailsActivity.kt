package com.violas.wallet.ui.message.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import com.palliums.utils.formatDate
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.base.BaseWebActivity
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.repository.http.message.SystemMsgDetailsDTO
import kotlinx.android.synthetic.main.activity_system_msg_details.*

/**
 * Created by elephant on 2/2/21 11:02 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class SystemMsgDetailsActivity : BaseWebActivity() {

    companion object {
        fun start(context: Context, msgDetails: SystemMsgDetailsDTO) {
            Intent(context, SystemMsgDetailsActivity::class.java)
                .apply { putExtra(KEY_ONE, msgDetails) }
                .start(context)
        }
    }

    private lateinit var msgDetails: SystemMsgDetailsDTO

    override fun onCreate(savedInstanceState: Bundle?) {
        if (initData(savedInstanceState)) {
            super.onCreate(savedInstanceState)
            tvTitle.text = msgDetails.title
            tvTime.text = formatDate(msgDetails.time, pattern = "yyyy-MM-dd&#160;&#160;HH:mm")
            tvAuthor.text = if (msgDetails.author.isNotBlank())
                msgDetails.author
            else
                getString(R.string.system_msg_details_desc_author_default)
        } else {
            finish()
        }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        var msgDetails: SystemMsgDetailsDTO? = null
        if (intent != null) {
            msgDetails = intent.getParcelableExtra(KEY_ONE)
        }

        return if (msgDetails != null) {
            this.msgDetails = msgDetails
            true
        } else {
            false
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_system_msg_details
    }

    override fun getUrl(): String {
        return ""
    }

    override fun getFixedTitle(): String? {
        return getString(R.string.system_msg_details_title)
    }

    override fun getWebView(): WebView? {
        return vWeb
    }

    override fun getProgressView(): ProgressBar? {
        return vProgress
    }

    override fun getFailedView(): View? {
        return vFailed
    }

    override fun startLoad() {
        getProgressView()?.progress = 0
        getWebView()?.visibility = View.VISIBLE
        val data = Html.fromHtml(msgDetails.content).toString()
        getWebView()?.loadData(data, "text/html", "UTF-8")
    }

    override fun onPageStarted() {
        getFailedView()?.visibility = View.GONE
        getProgressView()?.visibility = View.VISIBLE
    }

    override fun onLoadError() {
        getFailedView()?.visibility = View.VISIBLE
        getWebView()?.visibility = View.GONE
    }
}