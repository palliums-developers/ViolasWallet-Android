package com.violas.wallet.ui.market

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.palliums.utils.formatDate
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.getResourceId
import com.palliums.utils.start
import com.palliums.violas.http.MarketSwapRecordDTO
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.utils.convertViolasTokenUnit
import kotlinx.android.synthetic.main.activity_swap_details.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Created by elephant on 2020/7/15 09:42.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易市场兑换详情页面
 */
class SwapDetailsActivity : BaseAppActivity() {

    companion object {
        fun start(context: Context, record: MarketSwapRecordDTO) {
            Intent(context, SwapDetailsActivity::class.java)
                .apply { putExtra(KEY_ONE, record) }
                .start(context)
        }
    }

    private lateinit var mSwapRecord: MarketSwapRecordDTO

    override fun getLayoutResId(): Int {
        return R.layout.activity_swap_details
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.swap_details)
        if (initData(savedInstanceState)) {
            initView(mSwapRecord)
        } else {
            close()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_ONE, mSwapRecord)
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        var record: MarketSwapRecordDTO? = null
        if (savedInstanceState != null) {
            record = savedInstanceState.getParcelable(KEY_ONE)
        } else if (intent != null) {
            record = intent.getParcelableExtra(KEY_ONE)
        }

        return if (record == null) {
            false
        } else {
            mSwapRecord = record
            true
        }
    }

    private fun initView(swapRecord: MarketSwapRecordDTO) {
        tvFromToken.text = "${convertViolasTokenUnit(swapRecord.fromAmount)} ${swapRecord.fromName}"
        tvToToken.text = "${convertViolasTokenUnit(swapRecord.toAmount)} ${swapRecord.toName}"
        tvExchangeRate.text = run {
            val rate = BigDecimal(swapRecord.toAmount).divide(
                BigDecimal(swapRecord.fromAmount),
                8,
                RoundingMode.DOWN
            ).stripTrailingZeros().toPlainString()

            "1:$rate"
        }
        tvHandlingFee.text = getString(R.string.value_null)
        tvGasFee.text = getString(R.string.value_null)
        tvOrderTime.text = formatDate(swapRecord.date, pattern = "yyyy-MM-dd HH:mm:ss")
        tvDealTime.text = getString(R.string.value_null)

        tvProcessingDesc.setTextColor(
            getColorByAttrId(R.attr.marketDetailsCompletedStateTextColor, this)
        )
        vVerticalLine2.setBackgroundColor(
            getColorByAttrId(R.attr.marketDetailsCompletedLineBgColor, this)
        )
        vVerticalLine2.visibility = View.VISIBLE
        ivResultIcon.visibility = View.VISIBLE
        tvResultDesc.visibility = View.VISIBLE

        // 兑换成功
        if (swapRecord.status == 4001) {
            tvResultDesc.setText(R.string.market_swap_state_succeeded)
            tvResultDesc.setTextColor(
                getColorByAttrId(R.attr.textColorSuccess, this)
            )
            ivResultIcon.setBackgroundResource(
                getResourceId(R.attr.iconRecordStateSucceeded, this)
            )
            return
        }

        // 兑换失败
        tvResultDesc.setText(R.string.market_swap_state_failed)
        tvResultDesc.setTextColor(
            getColorByAttrId(R.attr.textColorFailure, this)
        )
        ivResultIcon.setBackgroundResource(
            getResourceId(R.attr.iconRecordStateFailed, this)
        )
        tvRetry.visibility = View.VISIBLE
        tvRetry.setOnClickListener {
            // TODO 重试
        }
    }
}