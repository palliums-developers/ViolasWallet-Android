package com.violas.wallet.ui.setting

import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseWebActivity

/**
 * Created by elephant on 2019-10-31 18:37.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 帮助与反馈页面
 */
class HelpFeedbackActivity : BaseWebActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitleRightText(R.string.common_title_feedback)
        setTitleRightTextColor(R.color.color_492AC5)
    }

    override fun getUrl(): String {
        // TODO 替换帮助与反馈url
        return getString(R.string.url_app_help_and_feedback)
    }

    override fun getFixedTitle(): String? {
        return getString(R.string.common_title_help_feedback)
    }

    override fun onTitleRightViewClick() {
        FeedbackDialog().show()
    }
}