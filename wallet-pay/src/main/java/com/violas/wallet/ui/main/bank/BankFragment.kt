package com.violas.wallet.ui.main.bank

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
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
import com.palliums.net.LoadState
import com.palliums.utils.DensityUtility
import com.palliums.utils.StatusBarUtil
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.getResourceId
import com.palliums.widget.adapter.FragmentPagerAdapterSupport
import com.violas.wallet.R
import com.violas.wallet.ui.bank.order.borrowing.BankBorrowingOrderActivity
import com.violas.wallet.ui.bank.order.deposit.BankDepositOrderActivity
import com.violas.wallet.ui.main.bank.BankViewModel.Companion.ACTION_LOAD_ACCOUNT_INFO
import com.violas.wallet.ui.main.bank.BankViewModel.Companion.ACTION_LOAD_BORROWING_PRODUCTS
import com.violas.wallet.ui.main.bank.BankViewModel.Companion.ACTION_LOAD_DEPOSIT_PRODUCTS
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.widget.popup.MenuPopup
import kotlinx.android.synthetic.main.fragment_bank.*
import kotlinx.android.synthetic.main.fragment_bank_content.*
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
    private lateinit var fragmentPagerAdapter: FragmentPagerAdapterSupport

    override fun getLayoutResId(): Int {
        return R.layout.fragment_bank
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clTopGroup.post { adapterViewHeight() }
        initFragmentPager()
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)

        initEvent()
        initObserver()
    }

    private fun adapterViewHeight() {
        val statusBarHeight = StatusBarUtil.getStatusBarHeight()
        val topViewHeight = clTopGroup.measuredHeight
        val bottomViewTopMargin =
            topViewHeight + statusBarHeight - DensityUtility.dp2px(requireContext(), 52)

        clTopGroup.setPadding(
            clTopGroup.paddingLeft,
            statusBarHeight,
            clTopGroup.paddingRight,
            clTopGroup.paddingBottom
        )
        llBottomGroup.layoutParams =
            (llBottomGroup.layoutParams as ConstraintLayout.LayoutParams).apply {
                topMargin = bottomViewTopMargin
            }
    }

    private fun initFragmentPager() {
        val fragments = mutableListOf<Fragment>()
        childFragmentManager.fragments.forEach {
            if (it is DepositMarketFragment || it is BorrowingMarketFragment) {
                fragments.add(it)
            }
        }
        if (fragments.isEmpty()) {
            fragments.add(DepositMarketFragment())
            fragments.add(BorrowingMarketFragment())
        }

        fragmentPagerAdapter = FragmentPagerAdapterSupport(childFragmentManager).apply {
            setFragments(fragments)
            setTitles(
                mutableListOf(
                    getString(R.string.deposit_market),
                    getString(R.string.borrowing_market)
                )
            )
        }

        viewPager.offscreenPageLimit = 1
        viewPager.adapter = fragmentPagerAdapter

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                updateTab(tab, false)
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                updateTab(tab, true)
            }
        })
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.post {
            val count = viewPager.adapter!!.count
            for (i in 0 until count) {
                tabLayout.getTabAt(i)?.let { tab ->
                    tab.setCustomView(R.layout.item_home_bank_market_tab_layout)
                    updateTab(tab, i == viewPager.currentItem)?.let {
                        it.text = tab.text
                    }
                }
            }
        }
    }

    private fun updateTab(tab: TabLayout.Tab, select: Boolean): TextView? {
        return tab.customView?.findViewById<TextView>(R.id.textView)?.also {
            it.setTextSize(TypedValue.COMPLEX_UNIT_DIP, if (select) 14f else 12f)
            it.typeface = Typeface.create(
                getString(if (select) R.string.font_family_title else R.string.font_family_normal),
                Typeface.NORMAL
            )
            it.setTextColor(
                getColorByAttrId(
                    if (select) android.R.attr.textColor else android.R.attr.textColorTertiary,
                    requireContext()
                )
            )
        }
    }

    private fun initEvent() {
        refreshLayout.setEnableOverScrollDrag(false)
        refreshLayout.setOnRefreshListener {
            val action = if (viewPager.currentItem == 0)
                ACTION_LOAD_ACCOUNT_INFO.or(ACTION_LOAD_DEPOSIT_PRODUCTS)
            else
                ACTION_LOAD_ACCOUNT_INFO.or(ACTION_LOAD_BORROWING_PRODUCTS)
            loadData(action)
        }

        ivMenu.setOnClickListener {
            showMenuPopup()
        }

        ivShowHideAmount.expandTouchArea(30)
        ivShowHideAmount.setOnClickListener {
            bankViewModel.toggleAmountShowHide()
        }

        tvMiningRule.expandTouchArea(20)
        tvMiningRule.setOnClickListener {
            // TODO 进入挖矿规则页面
            showToast(R.string.home_bank_mining_title)
        }
    }

    private fun loadData(action: Int) {
        bankViewModel.execute(action = action)
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
                BankDepositOrderActivity.start(context)
            } else {
                BankBorrowingOrderActivity.start(context)
            }
        }, 300)
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
        bankViewModel.borrowableLiveData.observe(viewLifecycleOwner, Observer {
            tvBorrowable.text = it
        })
        bankViewModel.totalEarningsLiveData.observe(viewLifecycleOwner, Observer {
            tvTotalEarnings.text = it
        })
        bankViewModel.yesterdayEarningsLiveData.observe(viewLifecycleOwner, Observer {
            tvYesterdayEarnings.text = it
        })

        bankViewModel.depositProductsLiveData.observe(viewLifecycleOwner, Observer {
            getDepositMarketFragment()?.setData(it)
        })
        bankViewModel.borrowingProductsLiveData.observe(viewLifecycleOwner, Observer {
            getBorrowingMarketFragment()?.setData(it)
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

                    var action = if (it) ACTION_LOAD_ACCOUNT_INFO else 0
                    if (bankViewModel.depositProductsLiveData.value.isNullOrEmpty())
                        action = action.or(ACTION_LOAD_DEPOSIT_PRODUCTS)
                    if (bankViewModel.borrowingProductsLiveData.value.isNullOrEmpty())
                        action = action.or(ACTION_LOAD_BORROWING_PRODUCTS)

                    if (action != 0) {
                        loadData(action)
                    }
                }
            }
        )
    }

    private fun getDepositMarketFragment(): DepositMarketFragment? {
        return if (this::fragmentPagerAdapter.isInitialized)
            fragmentPagerAdapter.getItem(0) as DepositMarketFragment?
        else
            null
    }

    private fun getBorrowingMarketFragment(): BorrowingMarketFragment? {
        return if (this::fragmentPagerAdapter.isInitialized)
            fragmentPagerAdapter.getItem(1) as BorrowingMarketFragment?
        else
            null
    }
}