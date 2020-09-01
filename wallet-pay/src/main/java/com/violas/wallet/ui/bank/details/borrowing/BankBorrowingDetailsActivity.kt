package com.violas.wallet.ui.bank.details.borrowing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.palliums.utils.*
import com.palliums.widget.adapter.FragmentPagerAdapterSupport
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.repository.http.bank.BorrowingInfoDTO
import com.violas.wallet.ui.bank.repayBorrow.RepayBorrowActivity
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
        fun start(context: Context, borrowingInfo: BorrowingInfoDTO) {
            Intent(context, BankBorrowingDetailsActivity::class.java)
                .apply { putExtra(KEY_ONE, borrowingInfo) }
                .start(context)
        }
    }

    private lateinit var violasAddress: String
    private lateinit var borrowingInfo: BorrowingInfoDTO

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
        outState.putParcelable(KEY_ONE, borrowingInfo)
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        if (savedInstanceState != null) {
            borrowingInfo = savedInstanceState.getParcelable(KEY_ONE) ?: return false
        } else if (intent != null) {
            borrowingInfo = intent.getParcelableExtra(KEY_ONE) ?: return false
        }

        violasAddress =
            AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())?.address
                ?: return false

        return true
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
        title = borrowingInfo.productName
        tvAmountToBeRepaid.text = convertAmountToDisplayAmountStr(borrowingInfo.borrowedAmount)
        tvCoinUnit.text = borrowingInfo.productName
    }

    private fun initTab() {
        viewPager.offscreenPageLimit = 2
        viewPager.adapter = FragmentPagerAdapterSupport(supportFragmentManager).apply {
            setFragments(
                mutableListOf<Fragment>(
                    BorrowingDetailFragment.newInstance(borrowingInfo.productId, violasAddress),
                    RepaymentDetailFragment.newInstance(borrowingInfo.productId, violasAddress),
                    LiquidationDetailFragment.newInstance(borrowingInfo.productId, violasAddress)
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
        val count = viewPager.adapter!!.count
        for (i in 0 until count) {
            tabLayout.getTabAt(i)?.let { tab ->
                tab.setCustomView(R.layout.item_bank_borrowing_details_tab_layout)
                updateTabView(tab, i == 0)?.let {
                    it.text = tab.text
                }
            }
        }
    }

    private fun updateTabView(tab: TabLayout.Tab, select: Boolean): TextView? {
        return tab.customView?.findViewById<TextView>(R.id.textView)?.also {
            it.textSize = if (select) 16f else 10f
            it.setTextColor(
                getColorByAttrId(
                    if (select) android.R.attr.textColor else android.R.attr.textColorTertiary,
                    this
                )
            )
        }
    }

    private fun initEvent() {
        btnGoRepayment.setOnClickListener {
            RepayBorrowActivity.start(
                this,
                borrowingInfo.productId
            )
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                updateTabView(tab, false)
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                updateTabView(tab, true)
            }
        })
    }
}