package com.violas.wallet.ui.incentivePlan.miningEarnings

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.palliums.utils.*
import com.palliums.widget.adapter.FragmentPagerAdapterSupport
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import kotlinx.android.synthetic.main.activity_mining_earnings_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 11/26/20 11:03 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 挖矿收益明细页面
 */
class MiningEarningsDetailsActivity : BaseAppActivity() {

    companion object {
        fun start(context: Context) {
            Intent(context, MiningEarningsDetailsActivity::class.java)
                .start(context)
        }
    }

    private lateinit var violasAddress: String

    override fun getLayoutResId(): Int {
        return R.layout.activity_mining_earnings_details
    }

    override fun getTitleStyle(): Int {
        return PAGE_STYLE_LIGHT_MODE_PRIMARY_NAV_BAR
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launch {
            if (initData()) {
                initTopView()
                initChildView()
            } else {
                close()
            }
        }
    }

    private suspend fun initData(): Boolean {
        val accountDO = withContext(Dispatchers.IO) {
            try {
                AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
            } catch (e: Exception) {
                null
            }
        }

        return if (accountDO != null) {
            violasAddress = accountDO.address
            true
        } else {
            false
        }
    }

    private fun initTopView() {
        setTitle(R.string.title_incentive_earnings_details)
        setTopBackgroundResource(getResourceId(R.attr.bgPageTopViewLight, this))
        /*setTopBackgroundHeight(
            StatusBarUtil.getStatusBarHeight() + DensityUtility.dp2px(this, 110)
        )*/
    }

    private fun initChildView() {
        val fragments = mutableListOf<Fragment>()
        supportFragmentManager.fragments.forEach {
            if (it is InviteMiningEarningsFragment
                || it is PoolMiningEarningsFragment
                || it is BankMiningEarningsFragment
            ) {
                fragments.add(it)
            }
        }

        if (fragments.isEmpty()) {
            fragments.add(InviteMiningEarningsFragment.newInstance(violasAddress))
            fragments.add(PoolMiningEarningsFragment.newInstance(violasAddress))
            fragments.add(BankMiningEarningsFragment.newInstance(violasAddress))
        }

        viewPager.offscreenPageLimit = 2
        viewPager.adapter = FragmentPagerAdapterSupport(supportFragmentManager).apply {
            setFragments(fragments)
            setTitles(
                mutableListOf(
                    getString(R.string.tab_invite_mining_earnings),
                    getString(R.string.tab_pool_mining_earnings),
                    getString(R.string.tab_bank_mining_earnings),
                )
            )
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
                    tab.setCustomView(R.layout.item_mining_earnings_details_tab_layout)
                    updateTab(tab, i == viewPager.currentItem)?.let {
                        it.text = tab.text
                    }
                }
            }
        }
    }

    private fun updateTab(tab: TabLayout.Tab, select: Boolean): TextView? {
        return tab.customView?.findViewById<TextView>(R.id.textView)?.also {
            it.setTextSize(TypedValue.COMPLEX_UNIT_DIP, if (select) 16f else 10f)
            it.typeface = Typeface.create(
                getString(if (select) R.string.font_family_title else R.string.font_family_normal),
                Typeface.NORMAL
            )
            it.setTextColor(
                getColorByAttrId(
                    if (select) android.R.attr.textColor else android.R.attr.textColorTertiary,
                    this
                )
            )
        }
    }
}