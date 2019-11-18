package com.violas.wallet.ui.setting

import android.os.Bundle
import android.view.View
import com.violas.wallet.BuildConfig
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import kotlinx.android.synthetic.main.activity_about_us.*

/**
 * Created by elephant on 2019-10-31 11:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 关于我们页面
 */
class AboutUsActivity : BaseAppActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_about_us
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.about_us_title)
        tvAppNameVersion.text = getString(
            R.string.setting_app_name_version,
            BuildConfig.VERSION_NAME
        )

        mivWebsite.setOnClickListener(this)
        mivEmail.setOnClickListener(this)
        mivWeChat.setOnClickListener(this)
        mivQQ.setOnClickListener(this)
        mivWeibo.setOnClickListener(this)
        mivTelegram.setOnClickListener(this)
        mivTwitter.setOnClickListener(this)
        mivFacebook.setOnClickListener(this)
    }

    override fun onViewClick(view: View) {
        // TODO 跳转
        when (view.id) {
            R.id.mivWebsite -> {

            }

            R.id.mivEmail -> {

            }

            R.id.mivWeChat -> {

            }

            R.id.mivQQ -> {

            }

            R.id.mivWeibo -> {

            }

            R.id.mivTelegram -> {

            }

            R.id.mivTwitter -> {

            }

            R.id.mivFacebook -> {

            }
        }
    }
}