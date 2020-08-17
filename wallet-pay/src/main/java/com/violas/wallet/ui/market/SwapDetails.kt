package com.violas.wallet.ui.market

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import com.palliums.extensions.expandTouchArea
import com.palliums.utils.formatDate
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.getResourceId
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.palliums.utils.*
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.repository.http.exchange.SwapRecordDTO
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.convertAmountToExchangeRate
import kotlinx.android.synthetic.main.activity_swap_details.*

/**
 * Created by elephant on 2020/7/15 09:42.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易市场兑换详情页面
 */
class SwapDetailsActivity : BaseAppActivity() {

    companion object {
        fun start(context: Context, record: SwapRecordDTO) {
            Intent(context, SwapDetailsActivity::class.java)
                .apply { putExtra(KEY_ONE, record) }
                .start(context)
        }
    }

    private lateinit var mSwapRecord: SwapRecordDTO

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
        var record: SwapRecordDTO? = null
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

    private fun initView(record: SwapRecordDTO) {
        tvInputCoin.text =
            if (record.inputCoinName.isNullOrBlank() || record.inputCoinAmount.isNullOrBlank())
                getString(R.string.value_null)
            else
                "${convertAmountToDisplayAmountStr(
                    record.inputCoinAmount,
                    CoinTypes.parseCoinType(record.inputCoinType)
                )} ${record.inputCoinName}"

        tvOutputCoin.text =
            if (record.outputCoinName.isNullOrBlank() || record.outputCoinAmount.isNullOrBlank())
                getString(R.string.value_null)
            else
                "${convertAmountToDisplayAmountStr(
                    record.outputCoinAmount,
                    CoinTypes.parseCoinType(record.outputCoinType)
                )} ${record.outputCoinName}"

        tvExchangeRate.text =
            if (record.inputCoinAmount.isNullOrBlank() || record.outputCoinAmount.isNullOrBlank())
                getString(R.string.value_null)
            else
                convertAmountToExchangeRate(
                    record.inputCoinAmount,
                    record.outputCoinAmount
                ).let {
                    if (it == null) getString(R.string.value_null) else "1:${it.toPlainString()}"
                }

        tvHandlingFee.text = getString(R.string.value_null)

        tvGasFee.text =
            if (record.gasCoinAmount.isNullOrBlank() || record.gasCoinName.isNullOrBlank())
                getString(R.string.value_null)
            else
                "${convertAmountToDisplayAmountStr(record.gasCoinAmount)} ${record.gasCoinName}"

        val pattern = "yyyy-MM-dd HH:mm:ss"
        if (record.inputCoinType == record.outputCoinType) {
            tvOrderTime.text = formatDate(
                correctDateLength(record.confirmedTime) - 1000,
                pattern = pattern
            )
            tvDealTime.text = formatDate(record.confirmedTime, pattern = pattern)
        } else {
            tvOrderTime.text = formatDate(record.time, pattern = pattern)
            tvDealTime.text = getString(R.string.value_null)
        }

        // 兑换中
        if (record.customStatus == SwapRecordDTO.Status.PROCESSING) {
            tvProcessingDesc.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            tvProcessingDesc.setTextColor(
                getColorByAttrId(android.R.attr.textColor, this)
            )
            vVerticalLine2.setBackgroundColor(
                getColorByAttrId(R.attr.marketDetailsUncompletedLineBgColor, this)
            )

            // TODO 取消先隐藏
            tvRetry.visibility = View.GONE
            tvRetry.expandTouchArea()
            tvRetry.setOnClickListener {
                // TODO 重试
            }
            return
        }

        tvProcessingDesc.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
        tvProcessingDesc.setTextColor(
            getColorByAttrId(R.attr.marketDetailsCompletedStateTextColor, this)
        )
        vVerticalLine2.setBackgroundColor(
            getColorByAttrId(R.attr.marketDetailsCompletedLineBgColor, this)
        )
        ivResultIcon.visibility = View.VISIBLE
        tvResultDesc.visibility = View.VISIBLE
        tvRetry.visibility = View.GONE

        when (record.customStatus) {
            SwapRecordDTO.Status.SUCCEEDED -> {
                tvResultDesc.setText(R.string.market_swap_state_succeeded)
                tvResultDesc.setTextColor(
                    getColorByAttrId(android.R.attr.textColor, this)
                )
                ivResultIcon.setBackgroundResource(
                    getResourceId(R.attr.iconRecordStateSucceeded, this)
                )
            }

            SwapRecordDTO.Status.CANCELLED -> {
                tvResultDesc.setText(R.string.market_swap_state_cancelled)
                tvResultDesc.setTextColor(
                    getColorByAttrId(android.R.attr.textColorTertiary, this)
                )
                ivResultIcon.setBackgroundResource(
                    getResourceId(R.attr.iconRecordStateCancelled, this)
                )
            }

            else -> {
                tvResultDesc.setText(R.string.market_swap_state_failed)
                tvResultDesc.setTextColor(
                    getColorByAttrId(R.attr.textColorFailure, this)
                )
                ivResultIcon.setBackgroundResource(
                    getResourceId(R.attr.iconRecordStateFailed, this)
                )
            }
        }
    }
}