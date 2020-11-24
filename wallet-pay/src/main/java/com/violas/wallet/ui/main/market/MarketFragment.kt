package com.violas.wallet.ui.main.market

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.palliums.base.BaseFragment
import com.palliums.utils.StatusBarUtil
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.getDrawableCompat
import com.palliums.utils.start
import com.palliums.widget.adapter.FragmentPagerAdapterSupport
import com.violas.wallet.R
import com.violas.wallet.event.MarketPageType
import com.violas.wallet.event.SwitchMarketPageEvent
import com.violas.wallet.ui.main.market.pool.MarketPoolFragment
import com.violas.wallet.ui.main.market.swap.SwapFragment
import com.violas.wallet.ui.market.MyPoolActivity
import com.violas.wallet.ui.market.PoolRecordActivity
import com.violas.wallet.ui.market.SwapRecordActivity
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

        initToolbar()
        initFragmentPager()
    }

    private fun initToolbar() {
        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(toolbar)
            it.supportActionBar?.setDisplayShowTitleEnabled(false)
            //it.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
        toolbar.layoutParams = (toolbar.layoutParams as ConstraintLayout.LayoutParams).apply {
            topMargin = StatusBarUtil.getStatusBarHeight()
        }
        toolbar.setNavigationOnClickListener {
            context?.let {
                Intent(it, MyPoolActivity::class.java).start(it)
            }
        }
    }

    private fun initFragmentPager() {
        val fragments = mutableListOf<Fragment>()
        childFragmentManager.fragments.forEach {
            if (it is SwapFragment || it is MarketPoolFragment) {
                fragments.add(it)
            }
        }
        if (fragments.isEmpty()) {
            fragments.add(SwapFragment())
            fragments.add(MarketPoolFragment())
        }

        viewPager.offscreenPageLimit = 1
        viewPager.adapter = FragmentPagerAdapterSupport(childFragmentManager).apply {
            setFragments(fragments)
            addTitle(getString(R.string.title_market_swap))
            addTitle(getString(R.string.title_market_pool))
        }

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
                    updateTab(tab, i == viewPager.currentItem)?.let {
                        it.text = tab.text
                    }
                }
            }
        }
    }

    private fun updateTab(tab: TabLayout.Tab, select: Boolean): TextView? {
        return getTabTextView(tab)?.also {
            tab.view.background = if (select)
                getDrawableCompat(R.drawable.shape_bg_market_tab_item_selected, requireContext())
            else
                null
            it.setTextColor(
                getColorByAttrId(
                    if (select) R.attr.colorOnPrimary else R.attr.colorPrimary,
                    requireContext()
                )
            )
        }
    }

    private fun getTabTextView(tab: TabLayout.Tab): TextView? {
        return try {
            val textViewField = tab.view.javaClass.getDeclaredField("textView")
            textViewField.isAccessible = true
            val textView = textViewField.get(tab.view) as TextView
            textViewField.isAccessible = false
            textView
        } catch (e: Exception) {
            null
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSwitchMarketPageEvent(event: SwitchMarketPageEvent) {
        when (event.marketPageType) {
            MarketPageType.Swap -> {
                tabLayout.getTabAt(0)?.select()
            }

            MarketPageType.Pool -> {
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
                        Intent(it, SwapRecordActivity::class.java).start(it)
                    } else {
                        Intent(it, PoolRecordActivity::class.java).start(it)
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
}