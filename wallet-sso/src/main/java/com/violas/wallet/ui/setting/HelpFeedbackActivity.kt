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

        setTitleRightText(R.string.help_feedback_menu)
        setTitleRightTextColor(R.color.def_text_btn)
    }

    override fun getUrl(): String {
        // TODO 替换帮助与反馈url
        return getString(R.string.help_feedback_url)
    }

    override fun getFixedTitle(): String? {
        return getString(R.string.help_feedback_title)
    }

    override fun onTitleRightViewClick() {
        FeedbackDialog().show()
    }
}