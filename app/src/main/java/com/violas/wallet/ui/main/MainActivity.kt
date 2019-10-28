package com.violas.wallet.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.violas.wallet.R
import com.violas.wallet.base.adapter.ViewPagerAdapter
import com.violas.wallet.ui.main.me.MeFragment
import com.violas.wallet.ui.main.quotes.QuotesFragment
import com.violas.wallet.ui.main.wallet.WalletFragment
import com.violas.wallet.utils.DensityUtility
import kotlinx.android.synthetic.main.activity_main.*
import qiu.niorgai.StatusBarCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val QUIT_CHECK_INTERNAL = 2000

        fun start(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    }

    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private var mQuitTimePoint: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarCompat.translucentStatusBar(this, true)
        setContentView(R.layout.activity_main)

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
                    it.setIcon(R.drawable.table_market_selected);
                }
                R.id.tab_me -> {
                    it.setIcon(R.drawable.table_me_selected);
                }
            }
            true
        }
        bottom_navigation.selectedItemId = bottom_navigation.menu.findItem(R.id.tab_wallet).itemId

        viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        viewPagerAdapter.addFragment(WalletFragment())
        viewPagerAdapter.addFragment(QuotesFragment())
        viewPagerAdapter.addFragment(MeFragment())

        view_pager.adapter = viewPagerAdapter
        view_pager.offscreenPageLimit = 3
        bottom_navigation.setupWithViewPager(view_pager)
    }

    private fun resetToDefaultIcon() {
        bottom_navigation.menu.findItem(R.id.tab_wallet).setIcon(R.drawable.table_wallet_normal)
        bottom_navigation.menu.findItem(R.id.tab_market).setIcon(R.drawable.table_market_normal)
        bottom_navigation.menu.findItem(R.id.tab_me).setIcon(R.drawable.table_me_normal)
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() - mQuitTimePoint > QUIT_CHECK_INTERNAL) {
            Toast.makeText(
                applicationContext, R.string.quit_confirmation,
                Toast.LENGTH_SHORT
            ).show()
            mQuitTimePoint = System.currentTimeMillis()
        } else {
            // work around for https://code.google.com/p/android/issues/detail?id=176265
            try {
                super.onBackPressed()
            } catch (ex: IllegalStateException) {
                super.supportFinishAfterTransition()
                ex.printStackTrace()
            }
        }
    }
}
