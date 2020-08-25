package com.violas.wallet.ui.bank.record

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.palliums.utils.getResourceId
import com.palliums.widget.refresh.IRefreshLayout
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import kotlinx.android.synthetic.main.activity_bank_record.*

/**
 * Created by elephant on 2020/8/25 15:25.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行存款/借款记录公共页面
 */
abstract class BaseBankRecordActivity<VO> : BasePagingActivity<VO>() {

    override fun getTitleStyle(): Int {
        return PAGE_STYLE_CUSTOM
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_bank_record
    }

    override fun getRecyclerView(): RecyclerView {
        return recyclerView
    }

    override fun getRefreshLayout(): IRefreshLayout? {
        return refreshLayout
    }

    override fun getStatusLayout(): IStatusLayout? {
        return statusLayout
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitleLeftImageResource(getResourceId(R.attr.iconBackTertiary, this))
        mPagingHandler.init()
    }
}