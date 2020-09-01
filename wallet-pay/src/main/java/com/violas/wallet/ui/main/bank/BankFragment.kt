package com.violas.wallet.ui.main.bank

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.enums.PopupAnimation
import com.palliums.base.BaseFragment
import com.palliums.extensions.expandTouchArea
import com.palliums.extensions.isNoNetwork
import com.palliums.net.LoadState
import com.palliums.utils.*
import com.palliums.widget.adapter.FragmentPagerAdapterSupport
import com.violas.wallet.R
import com.violas.wallet.ui.bank.order.borrowing.BankBorrowingOrderActivity
import com.violas.wallet.ui.bank.order.deposit.BankDepositOrderActivity
import com.violas.wallet.ui.main.bank.BankViewModel.Companion.ACTION_LOAD_ACCOUNT_INFO
import com.violas.wallet.ui.main.bank.BankViewModel.Companion.ACTION_LOAD_ALL
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.widget.popup.MenuPopup
import kotlinx.android.synthetic.main.fragment_bank.*
import kotlinx.coroutines.launch

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
    private val depositMarketFragment by lazy { DepositMarketFragment() }
    private val borrowingMarketFragment by lazy { BorrowingMarketFragment() }

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
        initObserver()
        initEvent()
        initTab()
    }

    private fun initView() {
        ivTopBg.layoutParams = (ivTopBg.layoutParams as ConstraintLayout.LayoutParams).apply {
            height = StatusBarUtil.getStatusBarHeight() + DensityUtility.dp2px(context, 210)
        }
        refreshLayout.layoutParams =
            (refreshLayout.layoutParams as ConstraintLayout.LayoutParams).apply {
                topMargin = StatusBarUtil.getStatusBarHeight()
            }

        ivShowHideAmount.expandTouchArea(30)
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
        bankViewModel.totalEarningsLiveData.observe(viewLifecycleOwner, Observer {
            tvTotalEarnings.text = it
        })
        bankViewModel.yesterdayEarningsLiveData.observe(viewLifecycleOwner, Observer {
            tvYesterdayEarnings.text = it
        })

        bankViewModel.depositProductsLiveData.observe(viewLifecycleOwner, Observer {
            depositMarketFragment.setData(it)
        })
        bankViewModel.borrowingProductsLiveData.observe(viewLifecycleOwner, Observer {
            borrowingMarketFragment.setData(it)
        })

        bankViewModel.tipsMessage.observe(viewLifecycleOwner, Observer {
            it.getDataIfNotHandled()?.let { msg ->
                if (msg.isNotEmpty()) {
                    showToast(msg)
                }
            }
        })
        bankViewModel.loadState.observe(viewLifecycleOwner, Observer {
            when (it.peekData().status) {
                LoadState.Status.RUNNING -> {

                }

                else -> {
                    refreshLayout.finishRefresh()
                }
            }
        })

        WalletAppViewModel.getViewModelInstance().mExistsAccountLiveData.observe(
            viewLifecycleOwner,
            Observer {
                launch {
                    bankViewModel.initAddress()
                    if (bankViewModel.depositProductsLiveData.value.isNullOrEmpty()
                        || bankViewModel.borrowingProductsLiveData.value.isNullOrEmpty()
                    ) {
                        loadData(ACTION_LOAD_ALL)
                    } else if (it) {
                        loadData(ACTION_LOAD_ACCOUNT_INFO)
                    }
                }
            }
        )
    }

    private fun loadData(action: Int) {
        bankViewModel.execute(action = action)
    }

    private fun initEvent() {
        refreshLayout.setOnRefreshListener {
            loadData(ACTION_LOAD_ALL)
        }

        ivMenu.setOnClickListener {
            showMenuPopup()
        }

        ivShowHideAmount.setOnClickListener {
            bankViewModel.toggleAmountShowHide()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                updateTabView(tab, false)
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                updateTabView(tab, true)
            }
        })
    }

    private fun initTab() {
        viewPager.adapter = FragmentPagerAdapterSupport(childFragmentManager).apply {
            setFragments(
                mutableListOf<Fragment>(
                    depositMarketFragment,
                    borrowingMarketFragment
                )
            )
            setTitles(
                mutableListOf(
                    getString(R.string.deposit_market),
                    getString(R.string.borrowing_market)
                )
            )
        }

        tabLayout.setupWithViewPager(viewPager)
        val count = viewPager.adapter!!.count
        for (i in 0 until count) {
            tabLayout.getTabAt(i)?.let { tab ->
                tab.setCustomView(R.layout.item_home_bank_market_tab_layout)
                updateTabView(tab, i == 0)?.let {
                    it.text = tab.text
                }
            }
        }
    }

    private fun updateTabView(tab: TabLayout.Tab, select: Boolean): TextView? {
        return tab.customView?.findViewById<TextView>(R.id.textView)?.also {
            it.textSize = if (select) 14f else 12f
            it.setTextColor(
                getColorByAttrId(
                    if (select) android.R.attr.textColor else android.R.attr.textColorTertiary,
                    requireContext()
                )
            )
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
                            getResourceId(R.attr.bankDepositOrderIcon, context!!),
                            R.string.deposit_order
                        ),
                        Pair(
                            getResourceId(R.attr.bankBorrowingOrderIcon, context!!),
                            R.string.borrowing_order
                        )
                    )
                ) { position ->
                    context?.let {
                        delayStartBankOrderPage(it, position == 0)
                    }
                }
            )
            .show()
    }

    private fun delayStartBankOrderPage(context: Context, depositOrder: Boolean) {
        ivMenu.postDelayed({
            if (depositOrder) {
                Intent(context, BankDepositOrderActivity::class.java).start(context)
            } else {
                Intent(context, BankBorrowingOrderActivity::class.java).start(context)
            }
        }, 300)
    }
}