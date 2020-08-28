package com.violas.wallet.ui.dexOrder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.violas.wallet.R
import com.violas.wallet.repository.http.dex.DexOrderDTO
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2019-12-06 17:26.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DexOrdersViewAdapter(
    private val onClickItem: ((DexOrderDTO) -> Unit)? = null,
    private val onClickRevokeOrder: ((DexOrderDTO, Int) -> Unit)? = null
) : PagingViewAdapter<DexOrderDTO>() {

    private val simpleDateFormat = SimpleDateFormat("MM.dd HH:mm:ss", Locale.ENGLISH)

    override fun onCreateViewHolderSupport(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<DexOrderDTO> {
        return DexOrderViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_dex_order,
                parent,
                false
            ),
            simpleDateFormat,
            onClickItem,
            onClickRevokeOrder
        )
    }
}