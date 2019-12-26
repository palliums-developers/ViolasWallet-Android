package com.violas.wallet.ui.dexOrder

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import com.palliums.utils.setIndicatorLineWidth
import com.palliums.widget.adapter.FragmentPagerAdapterSupport
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import kotlinx.android.synthetic.main.activity_dex_orders.*

/**
 * Created by elephant on 2019-12-06 11:11.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易中心订单页面
 */
class DexOrdersActivity : BaseAppActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_dex_orders
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.title_orders)

        tlTabs.setupWithViewPager(vpFragments)
        tlTabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(vpFragments))
        tlTabs.post {
            tlTabs.getTabAt(0)?.setText(R.string.title_tab_uncompleted)
            tlTabs.getTabAt(1)?.setText(R.string.title_tab_completed)
            tlTabs.setIndicatorLineWidth(50f, 50f)
        }

        vpFragments.adapter = FragmentPagerAdapterSupport(supportFragmentManager).apply {
            addFragment(DexOrdersFragment.newInstance(DexOrderState.UNFINISHED))
            addFragment(DexOrdersFragment.newInstance(DexOrderState.FINISHED))
        }
    }
}

