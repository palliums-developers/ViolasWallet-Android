package com.violas.wallet.ui.main.market.fundPool

import com.palliums.base.BaseFragment
import com.violas.wallet.R
import kotlinx.android.synthetic.main.fragment_fund_pool.*

/**
 * Created by elephant on 2020/6/23 17:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 市场资金池视图
 */
class FundPoolFragment : BaseFragment() {

    private var lazyInitTag = false

    override fun getLayoutResId(): Int {
        return R.layout.fragment_fund_pool
    }

    override fun onResume() {
        super.onResume()
        if (!lazyInitTag) {
            lazyInitTag = true
            onLazy2InitView()
        }
    }

    private fun onLazy2InitView() {
        tvExchangeRate.text = getString(R.string.exchange_rate_format, "- -")
        tvMyFundPoolAmount.text = getString(R.string.my_fund_pool_amount_format, "- -")

        llSwitchType.setOnClickListener {

        }

        llFirstSelectToken.setOnClickListener {

        }

        llSecondSelectToken.setOnClickListener {

        }

        btnPositive.setOnClickListener {

        }
    }
}