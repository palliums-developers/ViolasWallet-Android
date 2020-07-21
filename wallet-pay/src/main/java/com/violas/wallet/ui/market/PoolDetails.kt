package com.violas.wallet.ui.market

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.palliums.utils.formatDate
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.getResourceId
import com.palliums.utils.start
import com.palliums.violas.http.MarketPoolRecordDTO
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.utils.convertViolasTokenUnit
import kotlinx.android.synthetic.main.activity_pool_details.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Created by elephant on 2020/7/15 14:32.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易市场资金池详情页面
 */
class PoolDetailsActivity : BaseAppActivity() {

    companion object {
        fun start(context: Context, record: MarketPoolRecordDTO) {
            Intent(context, PoolDetailsActivity::class.java)
                .apply { putExtra(KEY_ONE, record) }
                .start(context)
        }
    }

    private lateinit var mPoolRecord: MarketPoolRecordDTO

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
        var record: MarketPoolRecordDTO? = null
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

    private fun initView(poolRecord: MarketPoolRecordDTO) {
        tvTokenA.text = "${convertViolasTokenUnit(poolRecord.coinAAmount)} ${poolRecord.coinAName}"
        tvTokenB.text = "${convertViolasTokenUnit(poolRecord.coinBAmount)} ${poolRecord.coinBName}"
        tvLiquidityToken.text =
            "${if (poolRecord.isAddLiquidity()) "+" else "-"} ${poolRecord.liquidityAmount}"
        tvExchangeRate.text = run {
            val rate = BigDecimal(poolRecord.coinBAmount).divide(
                BigDecimal(poolRecord.coinAAmount),
                8,
                RoundingMode.DOWN
            ).stripTrailingZeros().toPlainString()

            "1:$rate"
        }
        tvGasFee.text = getString(R.string.value_null)
        tvOrderTime.text = formatDate(poolRecord.date, pattern = "yyyy-MM-dd HH:mm:ss")
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

        // 转入转出成功
        if (poolRecord.status == 4001) {
            tvResultDesc.setText(
                if (poolRecord.isAddLiquidity())
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
            if (poolRecord.isAddLiquidity())
                R.string.market_pool_add_state_failed
            else
                R.string.market_pool_remove_state_failed)
        tvResultDesc.setTextColor(
            getColorByAttrId(R.attr.textColorFailure, this)
        )
        ivResultIcon.setBackgroundResource(
            getResourceId(R.attr.iconRecordStateFailed, this)
        )
    }
}