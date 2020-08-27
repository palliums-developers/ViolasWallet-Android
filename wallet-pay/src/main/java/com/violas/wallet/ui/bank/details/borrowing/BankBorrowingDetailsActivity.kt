package com.violas.wallet.ui.bank.details.borrowing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
import com.palliums.utils.DensityUtility
import com.palliums.utils.StatusBarUtil
import com.palliums.utils.getResourceId
import com.palliums.utils.start
import com.palliums.widget.adapter.FragmentPagerAdapterSupport
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.repository.http.bank.CurrBorrowingDTO
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import kotlinx.android.synthetic.main.activity_bank_borrowing_details.*

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

    private lateinit var currBorrowing: CurrBorrowingDTO

    override fun getTitleStyle(): Int {
        return PAGE_STYLE_CUSTOM
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_bank_borrowing_details
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (initData(savedInstanceState)) {
            initView()
        } else {
            close()
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

        return if (currBorrowing == null) {
            false
        } else {
            this.currBorrowing = currBorrowing
            true
        }
    }

    private fun initView() {
        title = currBorrowing.coinName
        setTitleLeftImageResource(getResourceId(R.attr.iconBackTertiary, this))
        setTopBackgroundResource(getResourceId(R.attr.bankDetailsTopBg, this))
        setTopBackgroundHeight(
            StatusBarUtil.getStatusBarHeight(this) +
                    DensityUtility.dp2px(this, 137)
        )

        tvAmountToBeRepaid.text = convertAmountToDisplayAmountStr(currBorrowing.borrowed)
        tvCoinUnit.text = currBorrowing.coinName

        viewPager.offscreenPageLimit = 2
        viewPager.adapter = FragmentPagerAdapterSupport(supportFragmentManager)
            .apply {
                //setFragments(fragments)
                setTitles(
                    mutableListOf(
                        getString(R.string.borrowing_details),
                        getString(R.string.repayment_details),
                        getString(R.string.liquidation_details)
                    )
                )
            }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                getTextView(tab)?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                getTextView(tab)?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
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
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.getTabAt(0)?.select()

        btnGoRepayment.setOnClickListener {
            // TODO 进入还款页面
            showToast("进入还款页面")
        }
    }
}