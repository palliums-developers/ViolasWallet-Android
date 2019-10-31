package com.violas.wallet.ui.setting

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.violas.wallet.BuildConfig
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.ui.web.WebCommonActivity
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
                // TODO 替换服务协议url
                WebCommonActivity.start(
                    this,
                    getString(R.string.service_agreement_url),
                    getString(R.string.service_agreement_title)
                )
            }

            R.id.mivAboutUs -> {
                startActivity(Intent(this, AboutUsActivity::class.java))
            }

            R.id.mivHelpFeedback -> {
                startActivity(Intent(this, HelpFeedbackActivity::class.java))
            }
        }
    }
}