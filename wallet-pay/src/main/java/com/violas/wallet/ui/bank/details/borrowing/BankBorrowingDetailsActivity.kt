package com.violas.wallet.ui.bank.details.borrowing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.palliums.utils.*
import com.palliums.widget.adapter.FragmentPagerAdapterSupport
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.repository.http.bank.CurrBorrowingDTO
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import kotlinx.android.synthetic.main.activity_bank_borrowing_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2020/8/27 16:39.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行借款详情页面
 */
class BankBorrowingDetailsActivity : BaseAppActivity() {

    companion object {
        fun start(context: Context, currBorrowing: CurrBorrowingDTO) {
            Intent(context, BankBorrowingDetailsActivity::class.java)
                .apply { putExtra(KEY_ONE, currBorrowing) }
                .start(context)
        }
    }

    private lateinit var violasAddress: String
    private lateinit var currBorrowing: CurrBorrowingDTO

    override fun getTitleStyle(): Int {
        return PAGE_STYLE_CUSTOM
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_bank_borrowing_details
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launch {
            val initResult = withContext(Dispatchers.IO) {
                initData(savedInstanceState)
            }

            if (initResult) {
                initTitleBar()
                initCurrBorrowingInfo()
                initEvent()
                initTab()
            } else {
                close()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_ONE, currBorrowing)
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        var currBorrowing: CurrBorrowingDTO? = null
        if (savedInstanceState != null) {
            currBorrowing = savedInstanceState.getParcelable(KEY_ONE)
        } else if (intent != null) {
            currBorrowing = intent.getParcelableExtra(KEY_ONE)
        }

        violasAddress =
            AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())?.address
                ?: return false

        return if (currBorrowing == null) {
            false
        } else {
            this.currBorrowing = currBorrowing
            true
        }
    }

    private fun initTitleBar() {
        setTitleLeftImageResource(getResourceId(R.attr.iconBackTertiary, this))
        setTopBackgroundResource(getResourceId(R.attr.bankDetailsTopBg, this))
        setTopBackgroundHeight(
            StatusBarUtil.getStatusBarHeight(this) +
                    DensityUtility.dp2px(this, 137)
        )
    }

    private fun initCurrBorrowingInfo() {
        title = currBorrowing.coinName
        tvAmountToBeRepaid.text = convertAmountToDisplayAmountStr(currBorrowing.borrowed)
        tvCoinUnit.text = currBorrowing.coinName
    }

    private fun initTab() {
        viewPager.offscreenPageLimit = 2
        viewPager.adapter = FragmentPagerAdapterSupport(supportFragmentManager).apply {
            setFragments(
                mutableListOf<Fragment>(
                    BorrowingDetailFragment.newInstance(currBorrowing.coinName, violasAddress),
                    RepaymentDetailFragment.newInstance(currBorrowing.coinName, violasAddress),
                    LiquidationDetailFragment.newInstance(currBorrowing.coinName, violasAddress)
                )
            )
            setTitles(
                mutableListOf(
                    getString(R.string.borrowing_details),
                    getString(R.string.repayment_details),
                    getString(R.string.liquidation_details)
                )
            )
        }

        tabLayout.setupWithViewPager(viewPager)
        tabLayout.getTabAt(0)?.select()
    }

    private fun initEvent() {
        btnGoRepayment.setOnClickListener {
            // TODO 进入还款页面
            showToast("进入还款页面")
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                getTextView(tab).run {
                    textSize = 10f
                    setTextColor(
                        getColorByAttrId(
                            android.R.attr.textColorTertiary,
                            this@BankBorrowingDetailsActivity
                        )
                    )
                }
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                getTextView(tab).run {
                    textSize = 16f
                    setTextColor(
                        getColorByAttrId(
                            android.R.attr.textColor,
                            this@BankBorrowingDetailsActivity
                        )
                    )
                }
            }

            private fun getTextView(tab: TabLayout.Tab): TextView {
                var textView = tab.customView as? TextView
                if (textView == null) {
                    textView = AppCompatTextView(this@BankBorrowingDetailsActivity).apply {
                        text = tab.text
                    }
                    tab.customView = textView
                }
                return textView
            }
        })
    }
}