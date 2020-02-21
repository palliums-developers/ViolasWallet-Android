package com.violas.wallet.ui.outsideExchange.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.palliums.utils.getColor
import com.violas.wallet.R
import kotlinx.android.synthetic.main.item_mapping_exchange_order.view.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2020-02-18 12:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MappingExchangeOrdersViewAdapter(
    retryCallback: () -> Unit
) : PagingViewAdapter<MappingExchangeOrderVO>(retryCallback, DiffCallback()) {

    private val simpleDateFormat = SimpleDateFormat("yy.MM.dd HH:mm", Locale.ENGLISH)

    override fun onCreateViewHolderSupport(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<out Any> {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_mapping_exchange_order,
                parent,
                false
            ),
            simpleDateFormat
        )
    }
}

private class ViewHolder(
    view: View,
    private val simpleDateFormat: SimpleDateFormat
) : BaseViewHolder<MappingExchangeOrderVO>(view) {

    override fun onViewBind(itemPosition: Int, itemData: MappingExchangeOrderVO?) {
        itemData?.let {
            itemView.tvTime.text = simpleDateFormat.format(it.time)
            itemView.tvAmount.text = it.amount
            itemView.tvName.text = it.coinName
            itemView.tvAddress.text = it.address
            when (it.status) {
                0 -> {
                    itemView.tvStatus.setText(R.string.status_processing)
                    itemView.tvStatus.setTextColor(getColor(R.color.color_5BBE75))
                }

                1 -> {
                    itemView.tvStatus.setText(R.string.state_completed)
                    itemView.tvStatus.setTextColor(getColor(R.color.black))
                }

                2 -> {
                    itemView.tvStatus.setText(R.string.status_failure)
                    itemView.tvStatus.setTextColor(getColor(R.color.color_FF6464))
                }

                else -> {
                    itemView.tvStatus.setText(R.string.status_unknown)
                    itemView.tvStatus.setTextColor(getColor(R.color.color_FFD701))
                }
            }
        }
    }
}

private class DiffCallback : DiffUtil.ItemCallback<MappingExchangeOrderVO>() {
    override fun areItemsTheSame(
        oldItem: MappingExchangeOrderVO,
        newItem: MappingExchangeOrderVO
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: MappingExchangeOrderVO,
        newItem: MappingExchangeOrderVO
    ): Boolean {
        return oldItem == newItem
    }
}