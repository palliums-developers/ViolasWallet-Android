package com.violas.wallet.ui.bank.details.borrowing

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.getResourceId
import com.palliums.utils.start
import com.palliums.widget.adapter.FragmentPagerAdapterSupport
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.event.UpdateBankBorrowedAmountEvent
import com.violas.wallet.repository.http.bank.BorrowingInfoDTO
import com.violas.wallet.ui.bank.repayBorrow.RepayBorrowActivity
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import kotlinx.android.synthetic.main.activity_bank_borrowing_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.math.BigDecimal

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

    override fun getLayoutResId(): Int {
        return R.layout.activity_bank_borrowing_details
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launch {
            if (initData(savedInstanceState)) {
                initTopView()
                initBorrowingInfoView()
                initEvent()
                initFragmentPager()
            } else {
                close()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_ONE, borrowingInfo)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe
    fun onUpdateBankBorrowedAmountEvent(event: UpdateBankBorrowedAmountEvent) {
        launch {
            borrowingInfo.borrowedAmount = event.borrowedAmount
            val borrowedAmount = BigDecimal(borrowingInfo.borrowedAmount)
            tvAmountToBeRepaid.text =
                convertAmountToDisplayAmountStr(borrowedAmount)
            flBottomView.visibility =
                if (borrowedAmount == BigDecimal.ZERO) View.GONE else View.VISIBLE
        }
    }

    private suspend fun initData(savedInstanceState: Bundle?): Boolean {
        if (savedInstanceState != null) {
            borrowingInfo = savedInstanceState.getParcelable(KEY_ONE) ?: return false
        } else if (intent != null) {
            borrowingInfo = intent.getParcelableExtra(KEY_ONE) ?: return false
        }

        val accountDO = withContext(Dispatchers.IO) {
            AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
        }

        return if (accountDO != null) {
            violasAddress = accountDO.address
            true
        } else {
            false
        }
    }

    private fun initTopView() {
        setTopBackgroundResource(getResourceId(R.attr.bgPageTopViewLight, this))
        /*setTopBackgroundHeight(
            StatusBarUtil.getStatusBarHeight() + DensityUtility.dp2px(this, 110)
        )*/
    }

    private fun initBorrowingInfoView() {
        title = borrowingInfo.productName
        tvAmountToBeRepaid.text = convertAmountToDisplayAmountStr(borrowingInfo.borrowedAmount)
        tvCoinUnit.text = borrowingInfo.productName
    }

    private fun initEvent() {
        EventBus.getDefault().register(this)

        btnGoRepayment.setOnClickListener {
            RepayBorrowActivity.start(
                this,
                borrowingInfo.productId
            )
        }
    }

    private fun initFragmentPager() {
        val fragments = mutableListOf<Fragment>()
        supportFragmentManager.fragments.forEach {
            if (it is CoinBorrowingRecordFragment
                || it is CoinRepaymentRecordFragment
                || it is CoinLiquidationRecordFragment
            ) {
                fragments.add(it)
            }
        }
        if (fragments.isEmpty()) {
            fragments.add(
                CoinBorrowingRecordFragment.newInstance(
                    borrowingInfo.productId,
                    violasAddress,
                    borrowingInfo.productName
                )
            )
            fragments.add(
                CoinRepaymentRecordFragment.newInstance(
                    borrowingInfo.productId,
                    violasAddress,
                    borrowingInfo.productName
                )
            )
            fragments.add(
                CoinLiquidationRecordFragment.newInstance(
                    borrowingInfo.productId,
                    violasAddress,
                    borrowingInfo.productName
                )
            )
        }

        viewPager.offscreenPageLimit = 2
        viewPager.adapter = FragmentPagerAdapterSupport(supportFragmentManager).apply {
            setFragments(fragments)
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
                    tab.setCustomView(R.layout.item_bank_borrowing_details_tab_layout)
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