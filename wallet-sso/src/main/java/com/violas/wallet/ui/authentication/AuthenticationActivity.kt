package com.violas.wallet.ui.authentication

import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity

/**
 * Created by elephant on 2019-11-19 19:24.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 身份认证页面
 */
class AuthenticationActivity : BaseAppActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_authentication
    }

    override fun getPageStyle(): Int {
        return PAGE_STYLE_DARK_TITLE_PLIGHT_CONTENT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.authentication_title)
        // TODO UI 逻辑
    }
}