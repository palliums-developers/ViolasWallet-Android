package com.violas.wallet.ui.setting

import android.os.Bundle
import android.view.View
import com.palliums.utils.openBrowser
import com.palliums.utils.openEmailClient
import com.palliums.widget.MenuItemView
import com.violas.wallet.BuildConfig
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.ui.web.WebCommonActivity
import com.violas.wallet.utils.ClipboardUtils
import kotlinx.android.synthetic.main.activity_about_us.*

/**
 * Created by elephant on 2019-10-31 11:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 关于我们页面
 */
class AboutUsActivity : BaseAppActivity(), View.OnLongClickListener {

    override fun getLayoutResId(): Int {
        return R.layout.activity_about_us
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.common_title_about_us)
        tvAppNameVersion.text = "${getString(R.string.app_name)} ${BuildConfig.VERSION_NAME}"

        mivWebsite.setOnClickListener(this)
        mivEmail.setOnClickListener(this)
        mivWeChat.setOnClickListener(this)
        mivTelegram.setOnClickListener(this)
        mivTwitter.setOnClickListener(this)
        mivFacebook.setOnClickListener(this)

        mivWebsite.setOnLongClickListener(this)
        mivEmail.setOnLongClickListener(this)
        mivWeChat.setOnLongClickListener(this)
        mivTelegram.setOnLongClickListener(this)
        mivTwitter.setOnLongClickListener(this)
        mivFacebook.setOnLongClickListener(this)

        mivWeChat.visibility = View.GONE
        mivTelegram.visibility = View.GONE
        mivTwitter.visibility = View.GONE
        mivFacebook.visibility = View.GONE
    }

    override fun onViewClick(view: View) {
        // TODO 跳转
        when (view.id) {
            R.id.mivWebsite -> {
                if (!openBrowser(this, mivWebsite.endDescText.trim())) {
                    WebCommonActivity.start(this, mivWebsite.endDescText.trim())
                }
            }

            R.id.mivEmail -> {
                try {
                    openEmailClient(
                        activity = this,
                        receiver = mivEmail.endDescText.trim(),
                        handleError = false
                    )
                } catch (e: Exception) {
                    ClipboardUtils.copy(this, mivEmail.endDescText.trim())
                }
            }

            R.id.mivWeChat -> {

            }

            R.id.mivTelegram -> {

            }

            R.id.mivTwitter -> {

            }

            R.id.mivFacebook -> {

            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        if (view is MenuItemView) {
            ClipboardUtils.copy(this, view.endDescText.trim())
        }

        return true
    }
}