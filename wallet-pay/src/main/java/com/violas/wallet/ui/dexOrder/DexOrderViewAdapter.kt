package com.violas.wallet.ui.dexOrder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.violas.wallet.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2019-12-06 17:26.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DexOrderViewAdapter(
    retryCallback: () -> Unit,
    private val showOrderDetails: Boolean,
    private val viewModel: DexOrderViewModel,
    private val onOpenOrderDetails: (DexOrderVO) -> Unit,
    private val onOpenBrowserView: (DexOrderVO) -> Unit,
    private val onClickRevokeOrder: (DexOrderVO, Int) -> Unit
) : PagingViewAdapter<DexOrderVO>(retryCallback, DexOrdersDiffCallback()) {

    var headerData: DexOrderVO? = null

    private val simpleDateFormat = SimpleDateFormat("MM.dd HH:mm:ss", Locale.ENGLISH)

    override fun onCreateViewHolderSupport(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<DexOrderVO> {
        return when (viewType) {
            R.layout.item_dex_order_details_header -> {
                DexOrderDetailsHeaderViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_dex_order_details_header,
                        parent,
                        false
                    ),
                    viewModel,
                    simpleDateFormat,
                    onOpenBrowserView,
                    onClickRevokeOrder
                )
            }

            R.layout.item_dex_order_details -> {
                DexOrderDetailsViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_dex_order_details,
                        parent,
                        false
                    ),
                    viewModel,
                    simpleDateFormat,
                    onOpenBrowserView
                )
            }

            else -> {
                DexOrdersViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_dex_order,
                        parent,
                        false
                    ),
                    viewModel,
                    simpleDateFormat,
                    onOpenOrderDetails,
                    onClickRevokeOrder
                )
            }
        }
    }

    override fun getItemViewTypeSupport(position: Int): Int {
        return if (showOrderDetails) {
            if (position == 0) {
                R.layout.item_dex_order_details_header
            } else {
                R.layout.item_dex_order_details
            }
        } else {
            R.layout.item_dex_order
        }
    }

    override fun isHeaderItem(viewType: Int): Boolean {
        return viewType == R.layout.item_dex_order_details_header
    }

    override fun getHeaderItem(position: Int, viewType: Int): Any? {
        return if (viewType == R.layout.item_dex_order_details_header) {
            headerData
        } else {
            null
        }
    }

    override fun getHeaderItemCount(): Int {
        return if (showOrderDetails) 1 else 0
    }
}

class DexOrdersDiffCallback : DiffUtil.ItemCallback<DexOrderVO>() {
    override fun areItemsTheSame(
        oldItem: DexOrderVO,
        newItem: DexOrderVO
    ): Boolean {
        return oldItem.dexOrderDTO.id == newItem.dexOrderDTO.id
    }

    override fun areContentsTheSame(
        oldItem: DexOrderVO,
        newItem: DexOrderVO
    ): Boolean {
        return oldItem == newItem
    }
}