package com.violas.wallet.ui.incentive.receiveRewards

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewModel
import com.palliums.utils.*
import com.violas.wallet.R
import com.violas.wallet.base.BaseViewModelActivity
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.event.ReceiveIncentiveRewardsEvent
import com.violas.wallet.ui.incentive.receiveRewards.ReceiveIncentiveRewardsViewModel.Companion.ACTION_GET_VERIFICATION_CODE
import com.violas.wallet.ui.incentive.receiveRewards.ReceiveIncentiveRewardsViewModel.Companion.ACTION_RECEIVE_REWARD
import com.violas.wallet.ui.selectCountryArea.CountryAreaVO
import com.violas.wallet.ui.selectCountryArea.SelectCountryAreaActivity
import kotlinx.android.synthetic.main.activity_receive_incentive_rewards.*
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

/**
 * Created by elephant on 11/25/20 3:15 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 领取激励奖励页面
 */
class ReceiveIncentiveRewardsActivity : BaseViewModelActivity() {

    companion object {
        private const val REQUEST_CODE_SELECT_COUNTRY_AREA = 0

        fun start(context: Context) {
            Intent(context, ReceiveIncentiveRewardsActivity::class.java)
                .start(context)
        }
    }

    private val viewModel by lazy {
        ViewModelProvider(this).get(ReceiveIncentiveRewardsViewModel::class.java)
    }

    private val countDownTimer by lazy {
        object : CountDownTimer(1000 * 60 * 2, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                tvGetVerificationCode.text = "${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                resetCountDownTimer()
            }
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_receive_incentive_rewards
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.title_phone_receive_reward)

        btnReceive.requestFocus()
        launch {
            viewModel.init()
            initView()
        }
    }

    private fun initView() {
        tvAreaCode.setOnClickListener(this)
        tvGetVerificationCode.setOnClickListener(this)
        btnReceive.setOnClickListener(this)

        viewModel.countryAreaLiveData.observe(this) {
            tvAreaCode.text = "+${it.areaCode}"

            tvAreaCode.post {
                val paddingStart =
                    tvAreaCode.measuredWidth + DensityUtility.dp2px(this, 11)
                etPhoneNumber.setPadding(paddingStart, 0, 0, 0)
            }
        }

        etVerificationCode.post {
            val paddingEnd =
                tvGetVerificationCode.measuredWidth + DensityUtility.dp2px(this, 11)
            etVerificationCode.setPadding(0, 0, paddingEnd, 0)

            tvGetVerificationCode.layoutParams =
                (tvGetVerificationCode.layoutParams as ConstraintLayout.LayoutParams).apply {
                    width = tvGetVerificationCode.measuredWidth
                }
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
        } else if (etInviterAddress.hasFocus()
            && etInviterAddress.text.toString().trim().isEmpty()
        ) {
            showSoftInput(etInviterAddress)
        }
    }

    override fun onPause() {
        super.onPause()
        hideSoftInput(etPhoneNumber)
    }

    override fun onDestroy() {
        countDownTimer.cancel()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_SELECT_COUNTRY_AREA) {
                val countryArea =
                    data?.getParcelableExtra<CountryAreaVO>(KEY_ONE)
                countryArea?.let { viewModel.countryAreaLiveData.value = it }
            }
        }
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.tvAreaCode -> {
                SelectCountryAreaActivity.start(this, REQUEST_CODE_SELECT_COUNTRY_AREA)
            }

            R.id.tvGetVerificationCode -> {
                if (viewModel.execute(
                        etPhoneNumber.text.toString().trim(),
                        action = ACTION_GET_VERIFICATION_CODE,
                        successCallback = {
                            startCountDown()
                            showSoftInput(etVerificationCode)
                        }
                    )
                ) {
                    clearFocusAndHideSoftInput()
                }
            }

            R.id.btnReceive -> {
                if (viewModel.execute(
                        etPhoneNumber.text.toString().trim(),
                        etVerificationCode.text.toString().trim(),
                        etInviterAddress.text.toString().trim(),
                        action = ACTION_RECEIVE_REWARD,
                        successCallback = {
                            launch {
                                CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
                                EventBus.getDefault().post(ReceiveIncentiveRewardsEvent())
                                close()
                            }
                        }
                    )
                ) {
                    clearFocusAndHideSoftInput()
                }
            }
        }
    }

    private fun clearFocusAndHideSoftInput() {
        etPhoneNumber.clearFocus()
        etVerificationCode.clearFocus()
        etInviterAddress.clearFocus()
        btnReceive.requestFocus()
        hideSoftInput(etPhoneNumber)
    }

    private fun startCountDown() {
        tvGetVerificationCode.isClickable = false
        tvGetVerificationCode.isEnabled = false
        tvGetVerificationCode.gravity = Gravity.CENTER
        tvGetVerificationCode.setTextColor(
            getColorByAttrId(
                android.R.attr.textColorSecondary,
                this
            )
        )
        countDownTimer.start()
    }

    private fun resetCountDownTimer() {
        tvGetVerificationCode.isClickable = true
        tvGetVerificationCode.isEnabled = true
        tvGetVerificationCode.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        tvGetVerificationCode.setText(R.string.action_get_verification_code)
        tvGetVerificationCode.setTextColor(
            getColorByAttrId(
                R.attr.colorPrimary,
                this
            )
        )
    }
}