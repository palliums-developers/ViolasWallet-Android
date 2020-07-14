package com.violas.wallet.ui.main.market

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.tabs.TabLayout
import com.palliums.base.BaseFragment
import com.palliums.utils.StatusBarUtil
import com.palliums.utils.start
import com.palliums.widget.adapter.FragmentPagerAdapterSupport
import com.violas.wallet.R
import com.violas.wallet.event.MarketPageType
import com.violas.wallet.event.SwitchMarketPageEvent
import com.violas.wallet.ui.main.market.fundPool.FundPoolFragment
import com.violas.wallet.ui.main.market.fundPool.MyFundPoolActivity
import com.violas.wallet.ui.main.market.swap.SwapFragment
import kotlinx.android.synthetic.main.fragment_market.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Created by elephant on 2020/6/23 14:11.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 首页市场视图
 */
class MarketFragment : BaseFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_market
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EventBus.getDefault().register(this)

        val appCompatActivity = activity as? AppCompatActivity
        appCompatActivity?.setSupportActionBar(toolbar)
        appCompatActivity?.supportActionBar?.setDisplayShowTitleEnabled(false)
//        appCompatActivity?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        toolbar.layoutParams = (toolbar.layoutParams as ConstraintLayout.LayoutParams).apply {
            topMargin = StatusBarUtil.getStatusBarHeight()
        }
        toolbar.setNavigationOnClickListener {
            context?.let {
                Intent(it, MyFundPoolActivity::class.java).start(it)
            }
        }

        viewPager.offscreenPageLimit = 1
        viewPager.adapter = FragmentPagerAdapterSupport(childFragmentManager).apply {
            addFragment(SwapFragment())
            addFragment(FundPoolFragment())
            addTitle(getString(R.string.title_swap))
            addTitle(getString(R.string.title_fund_pool))
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.view.background = null
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                tab.view.setBackgroundResource(R.drawable.shape_bg_market_tab_item_selected)
            }
        })
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.getTabAt(0)?.select()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSwitchMarketPageEvent(event: SwitchMarketPageEvent) {
        when (event.marketPageType) {
            MarketPageType.Swap -> {
                tabLayout.getTabAt(0)?.select()
            }

            MarketPageType.FundPool -> {
                tabLayout.getTabAt(1)?.select()
            }
        }
    }

    private fun isSwapTab(): Boolean {
        return viewPager.currentItem == 0
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_market, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.market_record -> {
                context?.let {
                    if (isSwapTab()) {
                        // TODO 进入兑换记录页面
                        showToast(R.string.swap_records)
                    } else {
                        // TODO 进入资金池记录页面
                        showToast(R.string.fund_pool_records)
                    }
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        EventBus.getDefault().unregister(this)
        super.onDestroyView()
    }

    override fun onResume() {
        StatusBarUtil.setLightStatusBarMode(requireActivity().window, true)
        super.onResume()
    }
}