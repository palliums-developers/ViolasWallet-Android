package com.violas.wallet.ui.verification

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewModel
import com.palliums.utils.DensityUtility
import com.palliums.utils.hideSoftInput
import com.palliums.utils.showSoftInput
import com.violas.wallet.R
import com.violas.wallet.base.BaseViewModelActivity
import com.violas.wallet.ui.verification.EmailVerificationViewModel.Companion.ACTION_BING_EMAIL
import com.violas.wallet.ui.verification.EmailVerificationViewModel.Companion.ACTION_GET_VERIFICATION_CODE
import com.violas.wallet.utils.CountDownTimerUtils
import kotlinx.android.synthetic.main.activity_email_verification.*

/**
 * Created by elephant on 2019-11-19 19:24.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 邮箱验证页面
 */
class EmailVerificationActivity : BaseViewModelActivity() {

    private val mViewModel by viewModels<EmailVerificationViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return EmailVerificationViewModel() as T
            }
        }
    }

    private val mCountDownTimerUtils by lazy {
        CountDownTimerUtils(tvGetVerificationCode, 1000 * 60 * 2, 1000)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_email_verification
    }

    override fun getPageStyle(): Int {
        return PAGE_STYLE_DARK_TITLE_PLIGHT_CONTENT
    }

    override fun getViewModel(): BaseViewModel {
        return mViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.verification_email_title)

        tvGetVerificationCode.setOnClickListener(this)
        btnBind.setOnClickListener(this)

        mViewModel.getVerificationCodeResult.observe(this, Observer {
            if (it) {
                mCountDownTimerUtils.start()
            }
        })
        mViewModel.bindEmailResult.observe(this, Observer {
            if (it) {
                setResult(Activity.RESULT_OK)
                close()
            }
        })

        etEmailAddress.requestFocus()

        // 动态设置EditText的左右padding
        etEmailAddress.post {
            val paddingLeft =
                tvLabelEmail.width - DensityUtility.dp2px(this, 5)
            etEmailAddress.setPadding(paddingLeft, 0, 0, 0)

            val paddingRight =
                tvGetVerificationCode.width + DensityUtility.dp2px(this, 15)
            etVerificationCode.setPadding(0, 0, paddingRight, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        if (etEmailAddress.hasFocus()
            && etEmailAddress.text.toString().trim().isEmpty()
        ) {
            showSoftInput(etEmailAddress)
        } else if (etVerificationCode.hasFocus()
            && etVerificationCode.text.toString().trim().isEmpty()
        ) {
            showSoftInput(etVerificationCode)
        }
    }

    override fun onPause() {
        super.onPause()
        hideSoftInput(etEmailAddress)
    }

    override fun onDestroy() {
        mCountDownTimerUtils.cancel()

        super.onDestroy()
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.tvGetVerificationCode -> {
                if (mViewModel.execute(
                        etEmailAddress.text.toString().trim(),
                        action = ACTION_GET_VERIFICATION_CODE
                    )
                ) {
                    etVerificationCode.requestFocus()
                }
            }

            R.id.btnBind -> {
                if (mViewModel.execute(
                        etEmailAddress.text.toString().trim(),
                        etVerificationCode.text.toString().trim(),
                        action = ACTION_BING_EMAIL
                    )
                ) {
                    hideSoftInput(etEmailAddress)
                }
            }
        }
    }
}