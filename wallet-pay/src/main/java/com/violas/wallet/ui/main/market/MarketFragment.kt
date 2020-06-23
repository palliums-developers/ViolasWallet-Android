package com.violas.wallet.ui.main.market

import android.graphics.Rect
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.palliums.base.BaseFragment
import com.palliums.utils.StatusBarUtil
import com.violas.wallet.R
import com.violas.wallet.event.MarketPageType
import com.violas.wallet.event.SwitchMarketPageEvent
import com.violas.wallet.ui.main.market.fundPool.FundPoolFragment
import com.violas.wallet.ui.main.market.swap.SwapFragment
import kotlinx.android.synthetic.main.fragment_market.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * Created by elephant on 2020/6/23 14:11.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 首页市场视图
 */
class MarketFragment : BaseFragment() {

    private var lazyInitTag = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_market
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_market, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.market_my_fund_pool -> {
                showToast(R.string.title_my_fund_pool)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroyView() {
        EventBus.getDefault().unregister(this)
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        StatusBarUtil.setLightStatusBarMode(requireActivity().window, true)
        if (!lazyInitTag) {
            lazyInitTag = true
            onLazy2InitView()
        }
    }

    private fun onLazy2InitView() {
        val appCompatActivity = activity as? AppCompatActivity
        appCompatActivity?.setSupportActionBar(toolbar)
        appCompatActivity?.supportActionBar?.setDisplayShowTitleEnabled(false)
        appCompatActivity?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        toolbar.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val rectangle = Rect()
                requireActivity().window.decorView.getWindowVisibleDisplayFrame(rectangle)
                val statusBarHeight = rectangle.top

                val toolbarLayoutParams =
                    toolbar.layoutParams as ConstraintLayout.LayoutParams
                toolbarLayoutParams.setMargins(
                    toolbarLayoutParams.leftMargin,
                    statusBarHeight,
                    toolbarLayoutParams.rightMargin,
                    toolbarLayoutParams.bottomMargin
                )
                toolbar.layoutParams = toolbarLayoutParams

                toolbar.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        viewPager.offscreenPageLimit = 1
        viewPager.adapter = MarketFragmentAdapter(
            childFragmentManager,
            mutableListOf(
                Pair(getString(R.string.title_swap), SwapFragment()),
                Pair(getString(R.string.title_fund_pool), FundPoolFragment())
            )
        )

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

    @Subscribe
    fun onSwitchHomePageEvent(event: SwitchMarketPageEvent) {
        when (event.marketPageType) {
            MarketPageType.Swap -> {
                tabLayout.getTabAt(0)?.select()
            }

            MarketPageType.FundPool -> {
                tabLayout.getTabAt(1)?.select()
            }
        }
    }

    class MarketFragmentAdapter(
        fragmentManager: FragmentManager,
        private val fragments: List<Pair<String, Fragment>>
    ) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return fragments[position].second
        }

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return fragments[position].first
        }
    }
}