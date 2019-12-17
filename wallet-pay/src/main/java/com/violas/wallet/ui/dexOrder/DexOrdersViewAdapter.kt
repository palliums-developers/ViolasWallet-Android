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
class DexOrdersViewAdapter(
    retryCallback: () -> Unit,
    private val onOpenOrderDetails: ((DexOrderVO) -> Unit)? = null,
    private val onClickRevokeOrder: ((DexOrderVO, Int) -> Unit)? = null
) : PagingViewAdapter<DexOrderVO>(retryCallback, DexOrdersDiffCallback()) {

    private val simpleDateFormat = SimpleDateFormat("MM.dd HH:mm:ss", Locale.ENGLISH)

    override fun onCreateViewHolderSupport(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<DexOrderVO> {
        return DexOrderViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_dex_order,
                parent,
                false
            ),
            simpleDateFormat,
            onOpenOrderDetails,
            onClickRevokeOrder
        )
    }
}

class DexOrdersDiffCallback : DiffUtil.ItemCallback<DexOrderVO>() {
    override fun areItemsTheSame(
        oldItem: DexOrderVO,
        newItem: DexOrderVO
    ): Boolean {
        return oldItem.dto.id == newItem.dto.id
    }

    override fun areContentsTheSame(
        oldItem: DexOrderVO,
        newItem: DexOrderVO
    ): Boolean {
        return oldItem == newItem
    }
}