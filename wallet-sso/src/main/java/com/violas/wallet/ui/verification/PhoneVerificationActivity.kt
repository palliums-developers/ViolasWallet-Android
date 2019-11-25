package com.violas.wallet.ui.verification

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewModel
import com.violas.wallet.R
import com.violas.wallet.base.BaseViewModelActivity
import com.violas.wallet.ui.verification.PhoneVerificationViewModel.Companion.ACTION_BING_PHONE_NUMBER
import com.violas.wallet.ui.verification.PhoneVerificationViewModel.Companion.ACTION_GET_VERIFICATION_CODE
import com.violas.wallet.utils.CountDownTimerUtils
import kotlinx.android.synthetic.main.activity_phone_verification.*

/**
 * Created by elephant on 2019-11-19 19:24.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 手机验证页面
 */
class PhoneVerificationActivity : BaseViewModelActivity() {

    private val mViewModel by viewModels<PhoneVerificationViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return PhoneVerificationViewModel() as T
            }
        }
    }

    private val mCountDownTimerUtils by lazy {
        CountDownTimerUtils(vGetVerificationCode, 1000 * 60 * 3, 1000)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_phone_verification
    }

    override fun getPageStyle(): Int {
        return PAGE_STYLE_DARK_TITLE_PLIGHT_CONTENT
    }

    override fun getViewModel(): BaseViewModel {
        return mViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.verification_phone_title)

        vPhoneAreaCode.setOnClickListener(this)
        vGetVerificationCode.setOnClickListener(this)
        vBind.setOnClickListener(this)

        vPhoneAreaCode.text = "+86"

        mViewModel.getVerificationCodeResult.observe(this, Observer {
            if (it) {
                mCountDownTimerUtils.start()
            }
        })
        mViewModel.bindPhoneNumberResult.observe(this, Observer {
            if (it) {
                setResult(Activity.RESULT_OK)
                vBind.postDelayed({
                    close()
                }, 2000)
            }
        })

        showSoftInput(vPhoneNumber)
    }

    override fun onDestroy() {
        mCountDownTimerUtils.cancel()

        super.onDestroy()
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.vPhoneAreaCode -> {
                vPhoneNumber.requestFocus()

            }

            R.id.vGetVerificationCode -> {
                if (mViewModel.execute(
                        vPhoneAreaCode.text.toString().trim().substring(1),
                        vPhoneNumber.text.toString().trim(),
                        action = ACTION_GET_VERIFICATION_CODE
                    )
                ) {
                    vVerificationCode.requestFocus()
                }
            }

            R.id.vBind -> {
                mViewModel.execute(
                    vPhoneAreaCode.text.toString().trim().substring(1),
                    vPhoneNumber.text.toString().trim(),
                    vVerificationCode.text.toString().trim(),
                    action = ACTION_BING_PHONE_NUMBER
                )
            }
        }
    }
}