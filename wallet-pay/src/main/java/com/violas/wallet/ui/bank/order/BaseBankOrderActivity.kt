package com.violas.wallet.ui.bank.order

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.palliums.utils.DensityUtility
import com.palliums.utils.getResourceId
import com.palliums.widget.dividers.RecyclerViewItemDividers
import com.palliums.widget.refresh.IRefreshLayout
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import kotlinx.android.synthetic.main.activity_bank_order.*

/**
 * Created by elephant on 2020/8/24 17:54.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行存款/借款订单公共页面
 */
abstract class BaseBankOrderActivity<VO> : BasePagingActivity<VO>() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_bank_order
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

        setTitleRightImageResource(getResourceId(R.attr.iconRecordPrimary, this))
        getPagingHandler().init()
        recyclerView.addItemDecoration(
            RecyclerViewItemDividers(
                top = DensityUtility.dp2px(this, 5),
                bottom = DensityUtility.dp2px(this, 5),
                left = DensityUtility.dp2px(this, 15),
                right = DensityUtility.dp2px(this, 15)
            )
        )
    }
}