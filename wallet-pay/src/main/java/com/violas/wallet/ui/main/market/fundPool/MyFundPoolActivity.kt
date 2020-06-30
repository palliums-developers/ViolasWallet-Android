package com.violas.wallet.ui.main.market.fundPool

import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.event.MarketPageType
import com.violas.wallet.event.SwitchMarketPageEvent
import kotlinx.android.synthetic.main.activity_my_fund_pool.*
import org.greenrobot.eventbus.EventBus

/**
 * Created by elephant on 2020/6/29 12:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 我的资金池页面
 */
class MyFundPoolActivity : BaseAppActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_my_fund_pool
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.title_my_fund_pool)

        btnTransferIn.setOnClickListener {
            EventBus.getDefault().post(SwitchMarketPageEvent(MarketPageType.FundPool))
            close()
        }

        btnTransferOut.setOnClickListener {
            EventBus.getDefault().post(SwitchMarketPageEvent(MarketPageType.FundPool))
            close()
        }
    }
}