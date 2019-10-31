package com.violas.wallet.ui.setting

import android.os.Bundle
import android.view.View
import com.violas.wallet.BuildConfig
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import kotlinx.android.synthetic.main.activity_setting.*

/**
 * Created by elephant on 2019-10-31 11:03.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 设置页面
 */
class SettingActivity : BaseActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_setting
    }

    override fun getTitleStyle(): Int {
        return TITLE_STYLE_GREY_BACKGROUND
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.setting_title)
        tvAppNameVersion.text = getString(
            R.string.setting_app_name_version,
            BuildConfig.VERSION_NAME
        )
        mivMultiLanguage.setOnClickListener(this)
        mivServiceAgreement.setOnClickListener(this)
        mivAboutUs.setOnClickListener(this)
        mivHelpFeedback.setOnClickListener(this)
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.mivMultiLanguage -> {

            }
            R.id.mivServiceAgreement -> {

            }
            R.id.mivAboutUs -> {

            }
            R.id.mivHelpFeedback -> {

            }
        }
    }
}