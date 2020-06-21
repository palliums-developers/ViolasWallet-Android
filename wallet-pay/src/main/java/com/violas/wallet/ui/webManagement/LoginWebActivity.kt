package com.violas.wallet.ui.webManagement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseActivity
import com.palliums.base.BaseFragment
import com.palliums.base.BaseViewModel
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.base.BaseViewModelActivity
import com.violas.wallet.biz.ScanLoginBean
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.utils.decryptAccount
import kotlinx.android.synthetic.main.activity_login_desktop.*

/**
 * Created by elephant on 2020/3/23 15:39.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 登录网页端页面
 */
class LoginWebActivity : BaseViewModelActivity() {

    companion object {
        private const val SCAN_LOGIN_TYPE_DESKTOP = 1
        const val SCAN_LOGIN_TYPE_WEB = 2

        fun start(context: Context, scanLoginBean: ScanLoginBean) {
            if (scanLoginBean.loginType == SCAN_LOGIN_TYPE_WEB) {
                Intent(context, LoginWebActivity::class.java)
                    .apply { putExtra(KEY_ONE, scanLoginBean.sessionId) }
                    .start(context)
                return
            }

            if (scanLoginBean.loginType == SCAN_LOGIN_TYPE_DESKTOP) {
                if (context is BaseActivity) {
                    context.showToast(R.string.tips_scan_login_desktop)
                } else if (context is BaseFragment) {
                    context.showToast(R.string.tips_scan_login_desktop)
                }
                return
            }

            if (context is BaseActivity) {
                context.showToast(R.string.tips_scan_login_not_support)
            } else if (context is BaseFragment) {
                context.showToast(R.string.tips_scan_login_not_support)
            }
        }
    }

    private lateinit var mSessionId: String
    private val mViewModel by lazy {
        ViewModelProvider(this, LoginWebViewModelFactory(mSessionId))
            .get(LoginWebViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_login_desktop
    }

    override fun getTitleStyle(): Int {
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
        setRootBackgroundColor(R.color.def_activity_vice_bg)
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

        val password = etLoginPwd.text.toString().trim()
        if (password.isEmpty()) {
            showToast(R.string.hint_input_login_pwd)
            return
        }

        decryptAccount(
            accountDO = accountDO,
            password = password,
            passwordErrorCallback = {
                if (tvLoginPwdErrorTips.visibility != View.VISIBLE) {
                    tvLoginPwdErrorTips.visibility = View.VISIBLE
                }
            }
        ) {
            mViewModel.execute {
                showToast(R.string.tips_scan_login_success)
                close()
            }
        }
    }
}