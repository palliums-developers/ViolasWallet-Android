package com.violas.wallet.ui.dexOrder.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.violas.wallet.R
import com.violas.wallet.repository.http.dex.DexOrderTradeDTO
import com.violas.wallet.ui.dexOrder.DexOrderVO
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2019-12-06 17:26.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DexOrderDetailsViewAdapter(
    retryCallback: () -> Unit,
    private val addHeader: Boolean = false,
    private val dexOrder: DexOrderVO,
    private val onOpenBrowserView: ((url: String?) -> Unit)? = null,
    private val onClickRevokeOrder: ((dexOrder: DexOrderVO, position: Int) -> Unit)? = null
) : PagingViewAdapter<DexOrderTradeDTO>(retryCallback, DexOrdersDiffCallback()) {

    private val simpleDateFormat = SimpleDateFormat("MM.dd HH:mm:ss", Locale.ENGLISH)

    override fun onCreateViewHolderSupport(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<out Any> {
        return when (viewType) {
            R.layout.item_dex_order_details_header -> {
                DexOrderDetailsHeaderViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_dex_order_details_header,
                        parent,
                        false
                    ),
                    simpleDateFormat,
                    onOpenBrowserView,
                    onClickRevokeOrder
                )
            }

            else -> {
                DexOrderTradeViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_dex_order_trade,
                        parent,
                        false
                    ),
                    dexOrder,
                    simpleDateFormat,
                    onOpenBrowserView
                )
            }
        }
    }

    override fun getItemViewTypeSupport(position: Int): Int {
        return if (position == 0 && addHeader) {
            R.layout.item_dex_order_details_header
        } else {
            R.layout.item_dex_order_trade
        }
    }

    override fun isHeaderItem(viewType: Int): Boolean {
        return viewType == R.layout.item_dex_order_details_header
    }

    override fun getHeaderItem(position: Int, viewType: Int): Any? {
        return if (viewType == R.layout.item_dex_order_details_header) {
            dexOrder
        } else {
            null
        }
    }

    override fun getHeaderItemCount(): Int {
        return if (addHeader) 1 else 0
    }
}

class DexOrdersDiffCallback : DiffUtil.ItemCallback<DexOrderTradeDTO>() {
    override fun areItemsTheSame(
        oldItem: DexOrderTradeDTO,
        newItem: DexOrderTradeDTO
    ): Boolean {
        return oldItem.version == newItem.version
    }

    override fun areContentsTheSame(
        oldItem: DexOrderTradeDTO,
        newItem: DexOrderTradeDTO
    ): Boolean {
        return oldItem == newItem
    }
}