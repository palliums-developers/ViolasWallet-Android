package com.violas.wallet.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.palliums.utils.DensityUtility
import com.palliums.widget.adapter.FragmentPagerAdapterSupport
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.event.HomePageModifyEvent
import com.violas.wallet.event.HomePageType
import com.violas.wallet.ui.main.applyFor.ApplyForSSOFragment
import com.violas.wallet.ui.main.me.MeFragment
import com.violas.wallet.ui.main.wallet.WalletFragment
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class MainActivity : BaseAppActivity() {

    companion object {
        private const val QUIT_CHECK_INTERNAL = 2000

        fun start(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    }

    override fun getPageStyle(): Int {
        return PAGE_STYLE_CUSTOM
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_main
    }

    private lateinit var viewPagerAdapter: FragmentPagerAdapterSupport
    private var mQuitTimePoint: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)

        bottom_navigation.setIconsMarginTop(DensityUtility.dp2px(this, 5f))
        bottom_navigation.enableAnimation(false)
        bottom_navigation.enableShiftingMode(false)
        bottom_navigation.enableItemShiftingMode(false)
        bottom_navigation.setTextSize(
            DensityUtility.px2sp(
                this,
                (DensityUtility.dp2px(this, 10f).toFloat())
            ).toFloat()
        )
        bottom_navigation.setIconSize(26F, 26F)
        bottom_navigation.itemIconTintList = null
        bottom_navigation.setOnNavigationItemSelectedListener {
            resetToDefaultIcon()
            when (it.itemId) {
                R.id.tab_wallet -> {
                    it.setIcon(R.drawable.table_wallet_selected);
                }
                R.id.tab_market -> {
                    it.setIcon(R.drawable.table_apply_selected);
                }
                R.id.tab_me -> {
                    it.setIcon(R.drawable.table_me_selected);
                }
            }
            true
        }
        bottom_navigation.selectedItemId = bottom_navigation.menu.findItem(R.id.tab_wallet).itemId

        viewPagerAdapter = FragmentPagerAdapterSupport(supportFragmentManager)
        viewPagerAdapter.addFragment(WalletFragment())
        viewPagerAdapter.addFragment(ApplyForSSOFragment())
        viewPagerAdapter.addFragment(MeFragment())

        view_pager.adapter = viewPagerAdapter
        view_pager.offscreenPageLimit = 3
        bottom_navigation.setupWithViewPager(view_pager)
    }

    private fun resetToDefaultIcon() {
        bottom_navigation.menu.findItem(R.id.tab_wallet).setIcon(R.drawable.table_wallet_normal)
        bottom_navigation.menu.findItem(R.id.tab_market).setIcon(R.drawable.table_apply_normal)
        bottom_navigation.menu.findItem(R.id.tab_me).setIcon(R.drawable.table_me_normal)
    }

    @Subscribe
    fun onHomePageModifyEvent(event: HomePageModifyEvent){
        when (event.index) {
            HomePageType.Home -> {
                view_pager.currentItem = 0
            }
            HomePageType.ApplyFor -> {
                view_pager.currentItem = 1
            }
            HomePageType.Me -> {
                view_pager.currentItem = 2
            }
        }
    }

    override fun onBackPressedSupport() {
        if (System.currentTimeMillis() - mQuitTimePoint > QUIT_CHECK_INTERNAL) {
            Toast.makeText(
                applicationContext, R.string.quit_confirmation,
                Toast.LENGTH_SHORT
            ).show()
            mQuitTimePoint = System.currentTimeMillis()
        } else {
            // work around for https://code.google.com/p/android/issues/detail?id=176265
            try {
                super.onBackPressedSupport()
            } catch (ex: IllegalStateException) {
                super.supportFinishAfterTransition()
                ex.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }
}
