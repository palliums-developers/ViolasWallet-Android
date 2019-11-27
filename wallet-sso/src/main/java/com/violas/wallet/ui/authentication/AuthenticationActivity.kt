package com.violas.wallet.ui.authentication

import android.os.Bundle
import android.view.View
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import kotlinx.android.synthetic.main.activity_authentication.*

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
        // TODO 逻辑

        vCountryAreaLabel.setOnClickListener(this)
        vCountryAreaInput.setOnClickListener(this)
        vSelectCountryAreaBtn.setOnClickListener(this)
        vPhotographFrontBtn.setOnClickListener(this)
        vDeleteFrontBtn.setOnClickListener(this)
        vPhotographBackBtn.setOnClickListener(this)
        vDeleteBackBtn.setOnClickListener(this)
        vSubmitBtn.setOnClickListener(this)
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.vCountryAreaInput,
            R.id.vSelectCountryAreaBtn -> {
                showToast("select country or area")
            }

            R.id.vPhotographFrontBtn -> {
                showToast("photograph id card front")
            }

            R.id.vDeleteFrontBtn -> {
                showToast("delete id card front")
            }

            R.id.vPhotographBackBtn -> {
                showToast("photograph id card back")
            }

            R.id.vDeleteBackBtn -> {
                showToast("delete id card back")
            }

            R.id.vSubmitBtn -> {
                showToast("submit")
            }
        }
    }
}