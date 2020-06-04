package com.violas.wallet.ui.tokenInfo

import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.view.get
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.tabs.TabLayout
import com.palliums.base.ViewController
import com.palliums.extensions.close
import com.palliums.extensions.setTitleToCenter
import com.palliums.extensions.show
import com.palliums.utils.CustomMainScope
import com.palliums.utils.StatusBarUtil
import com.palliums.widget.loading.LoadingDialog
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import com.violas.wallet.ui.record.TransactionRecordFragment
import com.violas.wallet.ui.record.TransactionRecordType
import com.violas.wallet.utils.ClipboardUtils
import com.violas.wallet.viewModel.bean.AssetsLibraCoinVo
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.android.synthetic.main.activity_token_info_temp.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.yokeyword.fragmentation.SupportActivity

/**
 * Created by elephant on 2020/6/3 15:27.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
@Suppress("INACCESSIBLE_TYPE")
class TokenInfoTempActivity : SupportActivity(), ViewController,
    CoroutineScope by CustomMainScope() {

    private lateinit var mAssetsVo: AssetsVo

    private var mLoadingDialog: LoadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_token_info_temp)

        initData(savedInstanceState)
        initEvent()
        initView()
    }

    private fun initData(savedInstanceState: Bundle?) {
        mAssetsVo =
            AssetsLibraCoinVo(
                id = 1,
                amount = 10233399,
                address = "dhhoiweidjoiejodjoiejodjo"
            )
        mAssetsVo.name = "vtoken"
    }

    private fun initView() {
        StatusBarUtil.setLightStatusBarMode(this, true)

        setSupportActionBar(toolbar)
        toolbar.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val rectangle = Rect()
                val window = window
                window.decorView.getWindowVisibleDisplayFrame(rectangle)
                val statusBarHeight = rectangle.top

                val toolbarLayoutParams =
                    toolbar.layoutParams as CollapsingToolbarLayout.LayoutParams
                toolbarLayoutParams.height = toolbarLayoutParams.height + statusBarHeight
                toolbar.layoutParams = toolbarLayoutParams
                toolbar.setPadding(0, statusBarHeight, 0, 0)

                val tokenInfoLayoutParams =
                    clTokenInfo.layoutParams as CollapsingToolbarLayout.LayoutParams
                tokenInfoLayoutParams.topMargin = toolbar.height + statusBarHeight
                clTokenInfo.layoutParams = tokenInfoLayoutParams

                toolbar.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        toolbar.setNavigationOnClickListener {
            onBackPressedSupport()
        }

        title = null
        tvTitle.text = mAssetsVo.name
        collapsingToolbarLayout.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                if (oldBottom == bottom) {
                    collapsingToolbarLayout.removeOnLayoutChangeListener(this)
                    toolbar.setTitleToCenter(tvTitle)
                }
            }
        })

        tvTokenName.text = mAssetsVo.name
        tvTokenAmount.text = mAssetsVo.getAmount().toString()
        tvFiatAmount.text = "≈\$0.00"
        tvTokenAddress.text = "dhhoiweidjoiejodjoiejodjo"

        viewPager.adapter = TransactionRecordFragmentAdapter(
            fragmentManager = supportFragmentManager,
            titles = arrayListOf(
                Pair(getString(R.string.label_all), TransactionRecordType.ALL),
                Pair(getString(R.string.label_transfer_in), TransactionRecordType.TRANSFER_IN),
                Pair(getString(R.string.label_transfer_out), TransactionRecordType.TRANSFER_OUT)
            )
        )
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.getTabAt(0)?.select()
    }

    private fun initEvent() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                getTextView(tab)?.let {
                    it.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
                }
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.setCurrentItem(tab.position, true)

                getTextView(tab)?.let {
                    it.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                }
            }

            private fun getTextView(tab: TabLayout.Tab): TextView? {
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
        })

        tvTokenAddress.setOnClickListener {
            ClipboardUtils.copy(this, "dhhoiweidjoiejodjoiejodjo")
        }

        btnTransfer.setOnClickListener {

        }

        btnCollection.setOnClickListener {

        }

        flExchange.setOnClickListener {

        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MultiLanguageUtility.attachBaseContext(newBase))
    }

    override fun showProgress(@StringRes resId: Int) {
        showProgress(getString(resId))
    }

    override fun showProgress(msg: String?) {
        launch {
            if (mLoadingDialog == null) {
                mLoadingDialog = LoadingDialog()
                    .setMessage(msg)
                mLoadingDialog!!.show(supportFragmentManager)
            } else {
                mLoadingDialog!!.setMessage(msg)
            }
        }
    }

    override fun dismissProgress() {
        launch {
            mLoadingDialog?.close()
            mLoadingDialog = null
        }
    }

    override fun showToast(@StringRes msgId: Int, duration: Int) {
        showToast(getString(msgId), duration)
    }

    override fun showToast(msg: String, duration: Int) {
        launch {
            Toast.makeText(this@TokenInfoTempActivity, msg, duration).show()
        }
    }

    inner class TransactionRecordFragmentAdapter(
        fragmentManager: FragmentManager,
        private val titles: List<Pair<String, TransactionRecordType>>
    ) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return supportFragmentManager.findFragmentByTag(titles[position].first)
                ?: TransactionRecordFragment.newInstance("", coinTypes = CoinTypes.Libra)
        }

        override fun getCount(): Int {
            return titles.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return titles[position].first
        }
    }
}