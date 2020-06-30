package com.violas.wallet.ui.main.market.fundPool

import android.animation.ObjectAnimator
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.enums.PopupAnimation
import com.palliums.base.BaseFragment
import com.palliums.utils.getColor
import com.palliums.utils.getColorByAttrId
import com.palliums.widget.popup.EnhancedPopupCallback
import com.violas.wallet.R
import com.violas.wallet.ui.main.market.MarketSwitchPopupView
import kotlinx.android.synthetic.main.fragment_fund_pool.*

/**
 * Created by elephant on 2020/6/23 17:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 市场资金池视图
 */
class FundPoolFragment : BaseFragment() {

    private var lazyInitTag = false

    private val operationModeArrowUpAnimator by lazy {
        ObjectAnimator.ofFloat(ivOperationModeArrow, "rotation", 0F, 180F)
            .setDuration(360)
    }
    private val operationModeArrowDownAnimator by lazy {
        ObjectAnimator.ofFloat(ivOperationModeArrow, "rotation", 180F, 360F)
            .setDuration(360)
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_fund_pool
    }

    override fun onResume() {
        super.onResume()
        if (!lazyInitTag) {
            lazyInitTag = true
            onLazy2InitView()
        }
    }

    private fun onLazy2InitView() {
        tvExchangeRate.text = getString(R.string.exchange_rate_format, "- -")
        tvMyFundPoolAmount.text = getString(R.string.my_fund_pool_amount_format, "- -")

        llSwitchOperationMode.setOnClickListener {
            showSwitchOperationModePopup()
        }

        llFirstSelectToken.setOnClickListener {

        }

        llSecondSelectToken.setOnClickListener {

        }

        btnPositive.setOnClickListener {

        }
    }

    private fun showSwitchOperationModePopup() {
        setOperationModeViewBgAndTextColor(true)
        showOperationModeArrowAnimation(true)
        XPopup.Builder(requireContext())
            .hasShadowBg(false)
            .popupAnimation(PopupAnimation.ScrollAlphaFromTop)
            .atView(llSwitchOperationMode)
            .setPopupCallback(
                object : EnhancedPopupCallback() {
                    override fun onDismissBefore() {
                        setOperationModeViewBgAndTextColor(false)
                        showOperationModeArrowAnimation(false)
                    }
                })
            .asCustom(
                MarketSwitchPopupView(
                    requireContext(),
                    0,
                    mutableListOf(
                        getString(R.string.transfer_in),
                        getString(R.string.transfer_out)
                    )
                ) {

                }
            )
            .show()
    }

    private fun setOperationModeViewBgAndTextColor(pressed: Boolean) {
        llSwitchOperationMode.setBackgroundResource(
            if (pressed)
                R.drawable.bg_market_switch_pressed
            else
                R.drawable.sel_bg_market_switch
        )
        ivOperationModeArrow.setBackgroundResource(
            if (pressed)
                R.drawable.shape_market_downward_pressed
            else
                R.drawable.sel_market_downward
        )
        tvOperationMode.setTextColor(
            if (pressed)
                getColorByAttrId(R.attr.colorOnPrimary, requireContext())
            else
                getColor(R.color.sel_color_market_switch, requireContext())
        )
    }

    private fun showOperationModeArrowAnimation(arrowUp: Boolean) {
        if (arrowUp) {
            operationModeArrowUpAnimator.start()
        } else {
            operationModeArrowDownAnimator.start()
        }
    }
}