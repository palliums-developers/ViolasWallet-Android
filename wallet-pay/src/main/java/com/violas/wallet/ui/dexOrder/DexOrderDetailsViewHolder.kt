package com.violas.wallet.ui.dexOrder

import android.view.View
import com.palliums.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_dex_order_details.view.*
import java.text.SimpleDateFormat

/**
 * Created by elephant on 2019-12-09 16:55.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DexOrderDetailsViewHolder(
    view: View,
    private val viewModel: DexOrderViewModel,
    private val simpleDateFormat: SimpleDateFormat,
    private val onClickBrowserQuery: (DexOrderVO) -> Unit
) : BaseViewHolder<DexOrderVO>(view) {

    init {
        itemView.tvBrowserQuery.setOnClickListener(this)
    }

    override fun onViewBind(itemIndex: Int, itemData: DexOrderVO?) {
        itemData?.let {
            // 未完成时为订单创建的时间，已完成时为订单更新的时间
            itemView.tvTime.text = simpleDateFormat.format(
                if (it.isOpen()) it.dexOrderDTO.date else it.dexOrderDTO.updateDate
            )

            // 若拿A换B，价格、数量、已成交数量均为B的数据
            itemView.tvPrice.text = it.getTokenPrice.toString()
            itemView.tvTotalAmount.text = it.dexOrderDTO.amountGet
        }
    }

    override fun onViewClick(view: View, itemIndex: Int, itemData: DexOrderVO?) {
        itemData?.let {
            when (view) {
                itemView.tvBrowserQuery -> {
                    onClickBrowserQuery.invoke(it)
                }
            }
        }
    }
}