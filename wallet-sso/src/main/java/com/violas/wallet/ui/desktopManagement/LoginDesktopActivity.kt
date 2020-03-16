package com.violas.wallet.ui.desktopManagement

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewModel
import com.violas.wallet.R
import com.violas.wallet.base.BaseViewModelActivity
import kotlinx.android.synthetic.main.activity_login_desktop.*

/**
 * Created by elephant on 2020/3/16 15:39.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 登录桌面端页面
 */
class LoginDesktopActivity : BaseViewModelActivity() {

    private val mViewModel by lazy {
        ViewModelProvider(this).get(LoginDesktopViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_login_desktop
    }

    override fun getViewModel(): BaseViewModel {
        return mViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        btnConfirmLogin.setOnClickListener {
            login()
        }

        tvCancelLogin.setOnClickListener {
            close()
        }

        etLoginPwd.doAfterTextChanged {
            if (mViewModel.mLoginPwdErrorLD.value == true) {
                mViewModel.mLoginPwdErrorLD.value = false
            }
        }

        mViewModel.mLoginPwdErrorLD.observe(this, Observer {
            tvLoginPwdErrorTips.visibility = if (it) View.VISIBLE else View.GONE
        })
    }

    private fun login() {
        // TODO 登录逻辑
        mViewModel.execute(
            etLoginPwd.text.toString().trim(),
            successCallback = {
                // 登录成功后的处理
            }
        )
    }
}