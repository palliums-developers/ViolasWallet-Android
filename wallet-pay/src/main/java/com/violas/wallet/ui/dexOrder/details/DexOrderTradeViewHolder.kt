package com.violas.wallet.ui.dexOrder.details

import android.view.View
import com.palliums.base.BaseViewHolder
import com.palliums.utils.formatDate
import com.violas.wallet.repository.http.dex.DexOrderTradeDTO
import com.violas.wallet.utils.convertViolasTokenPrice
import com.violas.wallet.utils.convertViolasTokenUnit
import kotlinx.android.synthetic.main.item_dex_order_trade.view.*
import java.text.SimpleDateFormat

/**
 * Created by elephant on 2019-12-09 16:55.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DexOrderTradeViewHolder(
    view: View,
    private val simpleDateFormat: SimpleDateFormat,
    private val onClickBrowserQuery: ((url: String?) -> Unit)? = null
) : BaseViewHolder<DexOrderTradeDTO>(view) {

    init {
        itemView.tvBrowserQuery.setOnClickListener(this)
    }

    override fun onViewBind(itemPosition: Int, itemData: DexOrderTradeDTO?) {
        itemData?.let {
            itemView.tvTime.text = formatDate(it.date, simpleDateFormat)
            itemView.tvPrice.text = convertViolasTokenPrice(it.price.toString())
            itemView.tvTotalAmount.text = convertViolasTokenUnit(it.amount)
        }
    }

    override fun onViewClick(view: View, itemPosition: Int, itemData: DexOrderTradeDTO?) {
        itemData?.let {
            when (view) {
                itemView.tvBrowserQuery -> {
                    onClickBrowserQuery?.invoke(null)
                }

                else -> {
                }
            }
        }
    }
}