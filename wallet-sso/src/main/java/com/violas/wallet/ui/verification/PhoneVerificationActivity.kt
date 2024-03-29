package com.violas.wallet.ui.verification

import android.app.Activity
import android.content.Intent
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
import com.violas.wallet.common.EXTRA_KEY_COUNTRY_AREA
import com.violas.wallet.ui.selectCountryArea.CountryAreaVO
import com.violas.wallet.ui.selectCountryArea.SelectCountryAreaActivity
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

    companion object {
        private const val REQUEST_CODE_SELECT_COUNTRY_AREA = 0
    }

    private val mViewModel by viewModels<PhoneVerificationViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return PhoneVerificationViewModel() as T
            }
        }
    }

    private val mCountDownTimerUtils by lazy {
        CountDownTimerUtils(tvGetVerificationCode, 1000 * 60 * 2, 1000)
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

        tvAreaCode.setOnClickListener(this)
        ivSelectAreaCode.setOnClickListener(this)
        tvGetVerificationCode.setOnClickListener(this)
        btnBind.setOnClickListener(this)

        mViewModel.getVerificationCodeResult.observe(this, Observer {
            if (it) {
                mCountDownTimerUtils.start()
            }
        })
        mViewModel.bindPhoneNumberResult.observe(this, Observer {
            if (it) {
                setResult(Activity.RESULT_OK)
                close()
            }
        })
        mViewModel.countryAreaVO.observe(this, Observer {
            tvAreaCode.text = "+${it.areaCode}"

            // 动态设置EditText的左右padding
            tvAreaCode.post {
                val paddingLeft =
                    tvAreaCode.width + ivSelectAreaCode.width - DensityUtility.dp2px(this, 5)
                etPhoneNumber.setPadding(paddingLeft, 0, 0, 0)
            }
        })

        etPhoneNumber.requestFocus()

        // 动态设置EditText的左右padding
        etPhoneNumber.post {
            val paddingRight =
                tvGetVerificationCode.width + DensityUtility.dp2px(this, 15)
            etVerificationCode.setPadding(0, 0, paddingRight, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        if (etPhoneNumber.hasFocus()
            && etPhoneNumber.text.toString().trim().isEmpty()
        ) {
            showSoftInput(etPhoneNumber)
        } else if (etVerificationCode.hasFocus()
            && etVerificationCode.text.toString().trim().isEmpty()
        ) {
            showSoftInput(etVerificationCode)
        }
    }

    override fun onPause() {
        super.onPause()
        hideSoftInput(etPhoneNumber)
    }

    override fun onDestroy() {
        mCountDownTimerUtils.cancel()

        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_SELECT_COUNTRY_AREA) {
                val countryAreaVO =
                    data?.getParcelableExtra<CountryAreaVO>(EXTRA_KEY_COUNTRY_AREA)
                countryAreaVO?.let { mViewModel.countryAreaVO.value = it }
            }
        }
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.tvAreaCode,
            R.id.ivSelectAreaCode -> {
                SelectCountryAreaActivity.start(
                    this, REQUEST_CODE_SELECT_COUNTRY_AREA, true
                )
            }

            R.id.tvGetVerificationCode -> {
                if (mViewModel.execute(
                        etPhoneNumber.text.toString().trim(),
                        action = ACTION_GET_VERIFICATION_CODE
                    )
                ) {
                    etVerificationCode.requestFocus()
                }
            }

            R.id.btnBind -> {
                if (mViewModel.execute(
                        etPhoneNumber.text.toString().trim(),
                        etVerificationCode.text.toString().trim(),
                        action = ACTION_BING_PHONE_NUMBER
                    )
                ) {
                    hideSoftInput(etPhoneNumber)
                }
            }
        }
    }
}