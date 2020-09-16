package com.violas.wallet.ui.market

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.palliums.utils.*
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.repository.http.exchange.PoolRecordDTO
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.convertAmountToExchangeRate
import com.violas.wallet.utils.getAmountPrefix
import kotlinx.android.synthetic.main.activity_pool_details.*
import java.math.BigDecimal

/**
 * Created by elephant on 2020/7/15 14:32.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易市场资金池详情页面
 */
class PoolDetailsActivity : BaseAppActivity() {

    companion object {
        fun start(context: Context, record: PoolRecordDTO) {
            Intent(context, PoolDetailsActivity::class.java)
                .apply { putExtra(KEY_ONE, record) }
                .start(context)
        }
    }

    private lateinit var mPoolRecord: PoolRecordDTO

    override fun getLayoutResId(): Int {
        return R.layout.activity_pool_details
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.pool_details)
        if (initData(savedInstanceState)) {
            initView(mPoolRecord)
        } else {
            close()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_ONE, mPoolRecord)
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        var record: PoolRecordDTO? = null
        if (savedInstanceState != null) {
            record = savedInstanceState.getParcelable(KEY_ONE)
        } else if (intent != null) {
            record = intent.getParcelableExtra(KEY_ONE)
        }

        return if (record == null) {
            false
        } else {
            mPoolRecord = record
            true
        }
    }

    private fun initView(record: PoolRecordDTO) {
        tvTokenA.text =
            if (record.coinAName.isNullOrBlank() || record.coinAAmount.isNullOrBlank())
                getString(R.string.value_null)
            else
                "${convertAmountToDisplayAmountStr(record.coinAAmount)} ${record.coinAName}"

        tvTokenB.text =
            if (record.coinBName.isNullOrBlank() || record.coinBAmount.isNullOrBlank())
                getString(R.string.value_null)
            else
                "${convertAmountToDisplayAmountStr(record.coinBAmount)} ${record.coinBName}"

        tvLiquidity.text =
            if (record.liquidityAmount.isNullOrBlank()) {
                getString(R.string.value_null)
            } else {
                val amount = BigDecimal(record.liquidityAmount)
                "${getAmountPrefix(
                    amount,
                    record.isAddLiquidity()
                )}${convertAmountToDisplayAmountStr(amount)}"
            }

        tvExchangeRate.text =
            if (record.coinAAmount.isNullOrBlank() || record.coinBAmount.isNullOrBlank())
                getString(R.string.value_null)
            else
                convertAmountToExchangeRate(record.coinAAmount, record.coinBAmount).let {
                    if (it == null) getString(R.string.value_null) else "1:${it.toPlainString()}"
                }

        tvGasFee.text =
            if (record.gasCoinAmount.isNullOrBlank() || record.gasCoinName.isNullOrBlank())
                getString(R.string.value_null)
            else
                "${convertAmountToDisplayAmountStr(record.gasCoinAmount)} ${record.gasCoinName}"

        tvOrderTime.text = formatDate(
            correctDateLength(record.confirmedTime) - 1000,
            pattern = "yyyy-MM-dd HH:mm:ss"
        )
        tvDealTime.text = formatDate(
            record.confirmedTime,
            pattern = "yyyy-MM-dd HH:mm:ss"
        )

        tvProcessingDesc.setTextColor(
            getColorByAttrId(R.attr.marketDetailsCompletedStateTextColor, this)
        )
        vVerticalLine2.setBackgroundColor(
            getColorByAttrId(R.attr.marketDetailsCompletedLineBgColor, this)
        )
        vVerticalLine2.visibility = View.VISIBLE
        ivResultIcon.visibility = View.VISIBLE
        tvResultDesc.visibility = View.VISIBLE

        // 转入转出成功
        if (record.isSuccess()) {
            tvResultDesc.setText(
                if (record.isAddLiquidity())
                    R.string.market_pool_add_state_succeeded
                else
                    R.string.market_pool_remove_state_succeeded
            )
            tvResultDesc.setTextColor(
                getColorByAttrId(R.attr.textColorSuccess, this)
            )
            ivResultIcon.setBackgroundResource(
                getResourceId(R.attr.iconRecordStateSucceeded, this)
            )
            return
        }

        // 转入转出失败
        tvResultDesc.setText(
            if (record.isAddLiquidity())
                R.string.market_pool_add_state_failed
            else
                R.string.market_pool_remove_state_failed
        )
        tvResultDesc.setTextColor(
            getColorByAttrId(R.attr.textColorFailure, this)
        )
        ivResultIcon.setBackgroundResource(
            getResourceId(R.attr.iconRecordStateFailed, this)
        )
    }
}