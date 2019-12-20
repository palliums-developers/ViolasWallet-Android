package com.violas.wallet.ui.setting

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.violas.wallet.BuildConfig
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.ui.changeLanguage.ChangeLanguageActivity
import com.violas.wallet.ui.web.WebCommonActivity
import com.violas.wallet.utils.feedbackByEmail
import kotlinx.android.synthetic.main.activity_setting.*

/**
 * Created by elephant on 2019-10-31 11:03.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 设置页面
 */
class SettingActivity : BaseAppActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_setting
    }

    override fun getPageStyle(): Int {
        return PAGE_STYLE_PLIGHT_TITLE_SLIGHT_CONTENT
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
        mivPrivacyPolicy.setOnClickListener(this)
        mivAboutUs.setOnClickListener(this)
        mivHelpFeedback.setOnClickListener(this)
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.mivMultiLanguage -> {
                ChangeLanguageActivity.start(this)
            }

            R.id.mivServiceAgreement -> {
                WebCommonActivity.start(
                    this,
                    getString(R.string.service_agreement_url),
                    getString(R.string.service_agreement_title)
                )
            }

            R.id.mivPrivacyPolicy -> {
                WebCommonActivity.start(
                    this,
                    getString(R.string.url_privacy_policy),
                    getString(R.string.title_privacy_policy)
                )
            }

            R.id.mivAboutUs -> {
                startActivity(Intent(this, AboutUsActivity::class.java))
            }

            R.id.mivHelpFeedback -> {
                //startActivity(Intent(this, HelpFeedbackActivity::class.java))
                //startActivity(Intent(this, FeedbackActivity::class.java))
                feedbackByEmail(this)
            }
        }
    }
}