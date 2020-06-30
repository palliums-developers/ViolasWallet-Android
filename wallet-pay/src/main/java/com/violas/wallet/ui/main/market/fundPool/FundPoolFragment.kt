package com.violas.wallet.ui.main.market.fundPool

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.enums.PopupAnimation
import com.palliums.base.BaseFragment
import com.palliums.utils.getColorByAttrId
import com.palliums.widget.popup.EnhancedPopupCallback
import com.violas.wallet.R
import com.violas.wallet.event.SwitchFundPoolOpModeEvent
import com.violas.wallet.ui.main.market.MarketSwitchPopupView
import kotlinx.android.synthetic.main.fragment_fund_pool.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * Created by elephant on 2020/6/23 17:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 市场资金池视图
 */
class FundPoolFragment : BaseFragment() {

    private val fundPoolViewModel by lazy {
        ViewModelProvider(this).get(FundPoolViewModel::class.java)
    }

    private val switchOpModeArrowUpAnimator by lazy {
        ObjectAnimator.ofFloat(ivSwitchOpModeArrow, "rotation", 0F, 180F)
            .setDuration(360)
    }
    private val switchOpModeArrowDownAnimator by lazy {
        ObjectAnimator.ofFloat(ivSwitchOpModeArrow, "rotation", 180F, 360F)
            .setDuration(360)
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_fund_pool
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)
        EventBus.getDefault().register(this)

        tvExchangeRate.text = getString(R.string.exchange_rate_format, "- -")
        tvMyFundPoolAmount.text = getString(R.string.my_fund_pool_amount_format, "- -")

        llSwitchOpModeGroup.setOnClickListener {
            showSwitchOpModePopup()
        }

        llFirstSelectGroup.setOnClickListener {

        }

        llSecondSelectGroup.setOnClickListener {

        }

        btnPositive.setOnClickListener {

        }

        fundPoolViewModel.getOpModeLiveData().observe(viewLifecycleOwner, Observer {
            if (it == FundPoolOpMode.TransferIn) {
                tvSwitchOpModeText.setText(R.string.transfer_in)
                tvFirstLabel.setText(R.string.transfer_in)
                tvSecondLabel.setText(R.string.transfer_in)
                tvSecondSelectText.setText(R.string.select_token)
                btnPositive.setText(R.string.action_transfer_in_nbsp)
                llFirstSelectGroup.visibility = View.VISIBLE
            } else {
                tvSwitchOpModeText.setText(R.string.transfer_out)
                tvFirstLabel.setText(R.string.fund_pool_token)
                tvSecondLabel.setText(R.string.transfer_out)
                tvSecondSelectText.setText(R.string.select_token_pair)
                btnPositive.setText(R.string.action_transfer_out_nbsp)
                llFirstSelectGroup.visibility = View.GONE
            }
        })
    }

    private fun showSwitchOpModePopup() {
        setSwitchOpModeViewBgAndTextColor(true)
        showSwitchOpModeArrowAnimation(true)
        XPopup.Builder(requireContext())
            .hasShadowBg(false)
            .popupAnimation(PopupAnimation.ScrollAlphaFromTop)
            .atView(llSwitchOpModeGroup)
            .setPopupCallback(
                object : EnhancedPopupCallback() {
                    override fun onDismissBefore() {
                        setSwitchOpModeViewBgAndTextColor(false)
                        showSwitchOpModeArrowAnimation(false)
                    }
                })
            .asCustom(
                MarketSwitchPopupView(
                    requireContext(),
                    fundPoolViewModel.getOpModelCurrPosition(),
                    mutableListOf(
                        getString(R.string.transfer_in),
                        getString(R.string.transfer_out)
                    )
                ) {
                    fundPoolViewModel.switchOpModel(FundPoolOpMode.values()[it])
                }
            )
            .show()
    }

    private fun setSwitchOpModeViewBgAndTextColor(pressed: Boolean) {
        llSwitchOpModeGroup.setBackgroundResource(
            if (pressed)
                R.drawable.bg_market_switch_pressed
            else
                R.drawable.sel_bg_market_switch
        )
        ivSwitchOpModeArrow.setBackgroundResource(
            if (pressed)
                R.drawable.shape_market_downward_pressed
            else
                R.drawable.sel_market_downward
        )
        if (pressed) {
            tvSwitchOpModeText.setTextColor(
                getColorByAttrId(R.attr.colorOnPrimary, requireContext())
            )
        } else {
            tvSwitchOpModeText.setTextColor(
                ContextCompat.getColorStateList(requireContext(), R.color.sel_color_market_switch)
            )
        }
    }

    private fun showSwitchOpModeArrowAnimation(arrowUp: Boolean) {
        if (arrowUp) {
            switchOpModeArrowUpAnimator.start()
        } else {
            switchOpModeArrowDownAnimator.start()
        }
    }

    @Subscribe(sticky = true)
    fun onSwitchFundPoolOpModeEvent(event: SwitchFundPoolOpModeEvent) {
        try {
            EventBus.getDefault().removeStickyEvent(event)
        } catch (ignore: Exception) {
        }

        fundPoolViewModel.switchOpModel(event.opMode)
    }

    override fun onDestroyView() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onDestroyView()
    }
}