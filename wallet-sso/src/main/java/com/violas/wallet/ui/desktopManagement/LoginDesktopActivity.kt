package com.violas.wallet.ui.desktopManagement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewModel
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.base.BaseViewModelActivity
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.utils.decryptAccount
import kotlinx.android.synthetic.main.activity_login_desktop.*

/**
 * Created by elephant on 2020/3/16 15:39.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 登录桌面端页面
 */
class LoginDesktopActivity : BaseViewModelActivity() {

    companion object {
        fun start(context: Context, sessionId: String) {
            Intent(context, LoginDesktopActivity::class.java)
                .apply { putExtra(KEY_ONE, sessionId) }
                .start(context)
        }
    }

    private lateinit var mSessionId: String
    private val mViewModel by lazy {
        ViewModelProvider(this, LoginDesktopViewModelFactory(mSessionId))
            .get(LoginDesktopViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_login_desktop
    }

    override fun getPageStyle(): Int {
        return PAGE_STYLE_CUSTOM
    }

    override fun getViewModel(): BaseViewModel {
        return mViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val sessionId = intent?.getStringExtra(KEY_ONE)
        if (sessionId.isNullOrEmpty()) {
            close()
            return
        }

        mSessionId = sessionId
        super.onCreate(savedInstanceState)

        setStatusBarMode(true)
        setRootBackgroundColor(R.color.def_page_bg_light_secondary)
        setTitleLeftViewVisibility(View.GONE)

        btnConfirmLogin.setOnClickListener {
            login()
        }
        tvCancelLogin.setOnClickListener {
            close()
        }
        etLoginPwd.doAfterTextChanged {
            if (tvLoginPwdErrorTips.visibility == View.VISIBLE) {
                tvLoginPwdErrorTips.visibility = View.GONE
            }
        }
    }

    private fun login() {
        val accountDO = mViewModel.mAccountLD.value ?: return

        val pwd = etLoginPwd.text.toString().trim()
        if (pwd.isEmpty()) {
            showToast(R.string.hint_input_login_pwd)
            return
        }

        decryptAccount(
            accountDO = accountDO,
            pwd = pwd,
            pwdErrorCallback = {
                if (tvLoginPwdErrorTips.visibility != View.VISIBLE) {
                    tvLoginPwdErrorTips.visibility = View.VISIBLE
                }
            }
        ) {
            mViewModel.execute(it) {
                showToast(R.string.tips_scan_login_success)
                close()
            }
        }
    }
}