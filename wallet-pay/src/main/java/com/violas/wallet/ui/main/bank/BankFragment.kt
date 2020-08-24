package com.violas.wallet.ui.main.bank

import android.content.Intent
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.enums.PopupAnimation
import com.palliums.base.BaseFragment
import com.palliums.extensions.expandTouchArea
import com.palliums.utils.DensityUtility
import com.palliums.utils.StatusBarUtil
import com.palliums.utils.getResourceId
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.ui.bank.order.deposit.BankDepositOrderActivity
import com.violas.wallet.widget.popup.MenuPopup
import kotlinx.android.synthetic.main.fragment_bank.*

/**
 * Created by elephant on 2020/8/19 15:38.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 首页-数字银行
 */
class BankFragment : BaseFragment() {

    private val bankViewModel by lazy {
        ViewModelProvider(this).get(BankViewModel::class.java)
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_bank
    }

    override fun onResume() {
        StatusBarUtil.setLightStatusBarMode(requireActivity().window, false)
        super.onResume()
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)

        initView()
        initEvent()
        initObserver()
    }

    private fun initView() {
        toolbar.layoutParams = (toolbar.layoutParams as ConstraintLayout.LayoutParams).apply {
            topMargin = StatusBarUtil.getStatusBarHeight()
        }

        ivTopBg.layoutParams = (ivTopBg.layoutParams as ConstraintLayout.LayoutParams).apply {
            height = StatusBarUtil.getStatusBarHeight() + DensityUtility.dp2px(context, 215)
        }

        ivShowHideAmount.expandTouchArea(30)
    }

    private fun initEvent() {
        ivMenu.setOnClickListener {
            showMenuPopup()
        }

        ivShowHideAmount.setOnClickListener {
            bankViewModel.toggleAmountShowHide()
        }
    }

    private fun initObserver() {
        bankViewModel.showAmountLiveData.observe(viewLifecycleOwner, Observer {
            ivShowHideAmount.setImageResource(
                getResourceId(
                    if (it) R.attr.iconShowPrimary else R.attr.iconHidePrimary,
                    context!!
                )
            )
        })

        bankViewModel.totalDepositLiveData.observe(viewLifecycleOwner, Observer {
            tvTotalDeposit.text = it
        })

        bankViewModel.totalBorrowableLiveData.observe(viewLifecycleOwner, Observer {
            tvTotalBorrowable.text = it
        })

        bankViewModel.totalIncomeLiveData.observe(viewLifecycleOwner, Observer {
            tvTotalIncome.text = it
        })

        bankViewModel.yesterdayIncomeLiveData.observe(viewLifecycleOwner, Observer {
            tvYesterdayIncome.text = it
        })
    }

    private fun showMenuPopup() {
        XPopup.Builder(requireContext())
            .hasShadowBg(false)
            .popupAnimation(PopupAnimation.ScaleAlphaFromRightTop)
            .atView(ivMenu)
            .offsetX(DensityUtility.dp2px(context, 12))
            .offsetY(DensityUtility.dp2px(context, -10))
            .asCustom(
                MenuPopup(
                    requireContext(),
                    mutableListOf(
                        Pair(
                            getResourceId(R.attr.bankDepositOrderIcon, context!!),
                            R.string.deposit_order
                        ),
                        Pair(
                            getResourceId(R.attr.bankBorrowOrderIcon, context!!),
                            R.string.borrow_order
                        )
                    )
                ) { position ->
                    context?.let {
                        if (position == 0) {
                            Intent(it, BankDepositOrderActivity::class.java).start(it)
                        } else {
                            showToast("进入借款订单页面")
                        }
                    }
                }
            )
            .show()
    }
}