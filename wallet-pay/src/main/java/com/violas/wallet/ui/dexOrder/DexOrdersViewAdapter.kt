package com.violas.wallet.ui.dexOrder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.violas.wallet.R
import com.violas.wallet.repository.http.dex.DexOrderDTO
import kotlinx.android.synthetic.main.item_dex_order.view.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2019-12-06 17:26.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DexOrdersViewAdapter(
    retryCallback: () -> Unit,
    private val viewModel: DexOrdersViewModel,
    private val onClickItem: (DexOrderDTO) -> Unit
) :
    PagingViewAdapter<DexOrderDTO>(retryCallback, DexOrdersDiffCallback()) {

    private val simpleDateFormat = SimpleDateFormat("MM.dd HH:mm:ss", Locale.CHINA)

    override fun onCreateViewHolderSupport(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<DexOrderDTO> {
        return DexOrdersViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_dex_order,
                parent,
                false
            ),
            viewModel,
            simpleDateFormat,
            onClickItem
        )
    }
}

class DexOrdersViewHolder(
    view: View,
    private val viewModel: DexOrdersViewModel,
    private val simpleDateFormat: SimpleDateFormat,
    private val onClickItem: (DexOrderDTO) -> Unit
) : BaseViewHolder<DexOrderDTO>(view) {

    init {
        itemView.setOnClickListener(this)
        itemView.tvRevoke.setOnClickListener(this)
    }

    override fun onViewBind(itemIndex: Int, itemData: DexOrderDTO?) {
        itemData?.let {
            itemView.tvBaseName.text = "BBBUSD / "
            itemView.tvQuoteName.text = "AAAUSD"
            itemView.tvTotalAmount.text = it.amountGive
            itemView.tvTime.text = it.date
            itemView.tvTradeAmount.text = it.amountFilled
            itemView.tvFee.text = "0.00Vtoken"
        }
    }

    override fun onViewClick(view: View, itemIndex: Int, itemData: DexOrderDTO?) {
        itemData?.let {
            when (view) {
                itemView -> {
                    onClickItem.invoke(it)
                }

                itemView.tvRevoke -> {
                    // TODO 撤销操作
                }
            }
        }
    }
}

class DexOrdersDiffCallback : DiffUtil.ItemCallback<DexOrderDTO>() {
    override fun areItemsTheSame(
        oldItem: DexOrderDTO,
        newItem: DexOrderDTO
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: DexOrderDTO,
        newItem: DexOrderDTO
    ): Boolean {
        return oldItem == newItem
    }
}