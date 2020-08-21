package com.violas.wallet.ui.main.bank

import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.enums.PopupAnimation
import com.palliums.base.BaseFragment
import com.palliums.utils.DensityUtility
import com.palliums.utils.StatusBarUtil
import com.palliums.utils.getResourceId
import com.violas.wallet.R
import com.violas.wallet.widget.popup.MenuPopup
import kotlinx.android.synthetic.main.fragment_bank.*

/**
 * Created by elephant on 2020/8/19 15:38.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 首页-数字银行
 */
class BankFragment : BaseFragment() {

    override fun getLayoutResId(): Int {
        return R.layout.fragment_bank
    }

    override fun onResume() {
        StatusBarUtil.setLightStatusBarMode(requireActivity().window, false)
        super.onResume()
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)

        toolbar.layoutParams = (toolbar.layoutParams as ConstraintLayout.LayoutParams).apply {
            topMargin = StatusBarUtil.getStatusBarHeight()
        }
        ivTopBg.layoutParams = (ivTopBg.layoutParams as ConstraintLayout.LayoutParams).apply {
            height = StatusBarUtil.getStatusBarHeight() + DensityUtility.dp2px(context, 215)
        }

        ivMenu.setOnClickListener {
            showMenuPopup()
        }
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
                            getResourceId(R.attr.homeBankMenuDepositOrderIcon, context!!),
                            R.string.deposit_order
                        ),
                        Pair(
                            getResourceId(R.attr.homeBankMenuBorrowOrderIcon, context!!),
                            R.string.borrow_order
                        )
                    )
                ) {
                    if (it == 0) {
                        showToast("进入存款订单页面")
                    } else {
                        showToast("进入借款订单页面")
                    }
                }
            )
            .show()
    }
}