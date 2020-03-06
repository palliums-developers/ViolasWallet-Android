package com.violas.wallet.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.WorkerThread
import com.palliums.utils.DensityUtility
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.WalletType
import com.violas.wallet.event.HomePageModifyEvent
import com.violas.wallet.event.HomePageType
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.ui.main.applyFor.ApplyForSSOFragment
import com.violas.wallet.ui.main.me.MeFragment
import com.violas.wallet.ui.main.message.ApplyMessageFragment
import com.violas.wallet.ui.main.wallet.WalletFragment
import com.violas.wallet.widget.adapter.FragmentPager2AdapterSupport
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.math.log

class MainActivity : BaseAppActivity() {

    private var mWalletType: WalletType? = null

    companion object {
        private const val QUIT_CHECK_INTERNAL = 2000

        fun start(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    private val mApplyForSSOFragment by lazy {
        ApplyForSSOFragment()
    }

    private val mApplyMessageFragment by lazy {
        ApplyMessageFragment()
    }

    override fun getPageStyle(): Int {
        return PAGE_STYLE_CUSTOM
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_main
    }

    private var viewPagerAdapter: FragmentPager2AdapterSupport? = null
    private var mQuitTimePoint: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        showProgress()
        launch(Dispatchers.IO) {
            loadWallet()
            withContext(Dispatchers.Main) {
                initView()
            }
            delay(600)
            dismissProgress()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSwitchAccountEvent(event: SwitchAccountEvent) {
        launch(Dispatchers.IO) {
            loadWallet()
            when (mWalletType) {
                WalletType.SSO -> {
                    viewPagerAdapter?.replaceFragment(1, ApplyForSSOFragment())
                }
                WalletType.Governor -> {
                    viewPagerAdapter?.replaceFragment(1, ApplyMessageFragment())
                }
                else -> {
                    viewPagerAdapter?.replaceFragment(1, ApplyForSSOFragment())
                }
            }
            withContext(Dispatchers.Main) {
                viewPagerAdapter?.notifyDataSetChanged()
                resetToDefaultIcon()

                bottom_navigation.menu.findItem(R.id.tab_wallet).setIcon(R.drawable.table_wallet_selected);
            }
        }
    }

    @WorkerThread
    private fun loadWallet() {
        mWalletType = WalletType.parse(mAccountManager.currentAccount().walletType)
    }

    private fun initView() {
        initViewListener()
        initTableView()
    }

    private fun initTableView() {
        viewPagerAdapter = FragmentPager2AdapterSupport(supportFragmentManager)
        viewPagerAdapter?.addFragment(WalletFragment())
        when (mWalletType) {
            WalletType.SSO -> {
                viewPagerAdapter?.addFragment(ApplyForSSOFragment())
            }
            WalletType.Governor -> {
                viewPagerAdapter?.addFragment(ApplyMessageFragment())
            }
            else -> {
                viewPagerAdapter?.addFragment(ApplyForSSOFragment())
            }
        }
        viewPagerAdapter?.addFragment(MeFragment())

        view_pager.adapter = viewPagerAdapter
        view_pager.offscreenPageLimit = 3
        bottom_navigation.setupWithViewPager(view_pager)
    }

    private fun initViewListener() {
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
                    when (mWalletType) {
                        WalletType.SSO -> {
                            it.setIcon(R.drawable.table_apply_selected);
                        }
                        WalletType.Governor -> {
                            it.setIcon(R.drawable.table_apply_message_selected);
                        }
                        else -> {
                            it.setIcon(R.drawable.table_apply_selected);
                        }
                    }
                }
                R.id.tab_me -> {
                    it.setIcon(R.drawable.table_me_selected);
                }
            }
            true
        }
        bottom_navigation.selectedItemId = bottom_navigation.menu.findItem(R.id.tab_wallet).itemId
    }

    private fun resetToDefaultIcon() {
        bottom_navigation.menu.findItem(R.id.tab_wallet).setIcon(R.drawable.table_wallet_normal)
        when (mWalletType) {
            WalletType.SSO -> {
                bottom_navigation.menu.findItem(R.id.tab_market)
                    .setIcon(R.drawable.table_apply_normal)
                    .title = getString(R.string.tab_applyfor)

            }
            WalletType.Governor -> {
                bottom_navigation.menu.findItem(R.id.tab_market)
                    .setIcon(R.drawable.table_apply_message_normal)
                    .title = getString(R.string.table_apply_message)
            }
            else -> {
                bottom_navigation.menu.findItem(R.id.tab_market)
                    .setIcon(R.drawable.table_apply_normal)
                    .title = getString(R.string.tab_applyfor)
            }
        }
        bottom_navigation.menu.findItem(R.id.tab_me).setIcon(R.drawable.table_me_normal)
    }

    @Subscribe
    fun onHomePageModifyEvent(event: HomePageModifyEvent) {
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
