package com.violas.wallet.ui.dexOrder

import android.view.View
import com.palliums.base.BaseViewHolder
import com.violas.wallet.repository.http.dex.DexOrderDTO
import kotlinx.android.synthetic.main.item_dex_order_part.view.*
import java.text.SimpleDateFormat

/**
 * Created by elephant on 2019-12-09 16:55.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DexOrdersPartInfoViewHolder(
    view: View,
    private val viewModel: DexOrdersViewModel,
    private val simpleDateFormat: SimpleDateFormat,
    private val onClickBrowserQuery: (DexOrderDTO) -> Unit
) : BaseViewHolder<DexOrderDTO>(view) {

    init {
        itemView.tvBrowserQuery.setOnClickListener(this)
    }

    override fun onViewBind(itemIndex: Int, itemData: DexOrderDTO?) {
        itemData?.let {
            itemView.tvTotalAmount.text = it.amountGet
        }
    }

    override fun onViewClick(view: View, itemIndex: Int, itemData: DexOrderDTO?) {
        itemData?.let {
            when (view) {
                itemView.tvBrowserQuery -> {
                    onClickBrowserQuery.invoke(it)
                }
            }
        }
    }
}