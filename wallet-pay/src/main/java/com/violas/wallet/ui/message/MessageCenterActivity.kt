package com.violas.wallet.ui.message

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.palliums.content.App
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.getResourceId
import com.palliums.utils.start
import com.palliums.widget.adapter.FragmentPagerAdapterSupport
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.ui.main.MainActivity
import kotlinx.android.synthetic.main.activity_message_center.*

/**
 * Created by elephant on 2020/10/10 17:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MessageCenterActivity : BaseAppActivity() {

    companion object {

        fun start(context: Context, extras: Bundle? = null) {
            Intent(context, MessageCenterActivity::class.java)
                .apply {
                    if (extras != null) {
                        putExtras(extras)
                    }
                }
                .start(context)
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_message_center
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.msg_center_title)
        setTitleRightImageResource(getResourceId(R.attr.iconClear, this))

        initFragmentPager()
    }

    override fun onTitleRightViewClick() {
        super.onTitleRightViewClick()
        // TODO 清除未读消息
    }

    override fun onBackPressedSupport() {
        // 首页未启动时，返回直接进入首页
        if (!App.existsActivity(MainActivity::class.java)) {
            MainActivity.start(this)
        }
        super.onBackPressedSupport()
    }

    private fun initFragmentPager() {
        val fragments = mutableListOf<Fragment>()
        supportFragmentManager.fragments.forEach {
            if (it is TransferNotificationFragment || it is SystemNotificationFragment) {
                fragments.add(it)
            }
        }
        if (fragments.isEmpty()) {
            fragments.add(TransferNotificationFragment())
            fragments.add(SystemNotificationFragment())
        }

        val fragmentPagerAdapter = FragmentPagerAdapterSupport(supportFragmentManager).apply {
            setFragments(fragments)
            setTitles(
                mutableListOf(
                    getString(R.string.msg_center_tab_transfer_notification),
                    getString(R.string.msg_center_tab_system_notification)
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
                // TODO test code, don't forget to delete
                showBadge(tab, false)
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                updateTab(tab, true)
                // TODO test code, don't forget to delete
                showBadge(tab, true)
            }
        })
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.post {
            val count = viewPager.adapter!!.count
            for (i in 0 until count) {
                tabLayout.getTabAt(i)?.let { tab ->
                    tab.setCustomView(R.layout.item_msg_center_tab_layout)
                    updateTab(tab, i == viewPager.currentItem)?.let {
                        it.text = tab.text
                    }
                }
            }
        }
    }

    private fun updateTab(tab: TabLayout.Tab, select: Boolean): TextView? {
        tab.customView?.findViewById<View>(R.id.vIndicator)?.visibility =
            if (select) View.VISIBLE else View.GONE
        return tab.customView?.findViewById<TextView>(R.id.textView)?.also {
            it.setTextColor(
                getColorByAttrId(
                    if (select) android.R.attr.textColor else android.R.attr.textColorTertiary,
                    this
                )
            )
        }
    }

    private fun showBadge(tab: TabLayout.Tab, show: Boolean) {
        tab.customView?.findViewById<View>(R.id.vBadge)?.visibility =
            if (show) View.VISIBLE else View.GONE
    }
}