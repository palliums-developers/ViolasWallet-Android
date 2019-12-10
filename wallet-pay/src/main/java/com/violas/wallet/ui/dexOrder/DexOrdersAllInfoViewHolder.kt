package com.violas.wallet.ui.dexOrder

import android.view.View
import com.palliums.base.BaseViewHolder
import com.palliums.utils.getColor
import com.violas.wallet.R
import com.violas.wallet.repository.http.dex.DexOrderDTO
import kotlinx.android.synthetic.main.item_dex_order_all.view.*
import java.text.SimpleDateFormat

/**
 * Created by elephant on 2019-12-09 16:53.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DexOrdersAllInfoViewHolder(
    view: View,
    private val viewModel: DexOrdersViewModel,
    private val simpleDateFormat: SimpleDateFormat,
    private val onClickItem: (DexOrderDTO) -> Unit
) : BaseViewHolder<DexOrderDTO>(view) {

    init {
        itemView.setOnClickListener(this)
        itemView.tvState.setOnClickListener(this)
    }

    override fun onViewBind(itemIndex: Int, itemData: DexOrderDTO?) {
        itemData?.let {
            itemView.tvBaseName.text = "BBBUSD / "
            itemView.tvQuoteName.text = "AAAUSD"
            itemView.tvTotalAmount.text = it.amountGive
            itemView.tvTime.text = it.date
            itemView.tvTradeAmount.text = it.amountFilled
            itemView.tvFee.text = "0.00Vtoken"

            if (it.isFinished()) {
                itemView.tvState.setText(if (it.isCanceled()) R.string.state_revoked else R.string.state_completed)
                itemView.tvState.setTextColor(getColor(R.color.color_63636F, itemView.context))
            } else if (it.isOpen()) {
                itemView.tvState.setText(R.string.action_revoke)
                itemView.tvState.setTextColor(getColor(R.color.color_726BD9, itemView.context))
            }
        }
    }

    override fun onViewClick(view: View, itemIndex: Int, itemData: DexOrderDTO?) {
        itemData?.let {
            when (view) {
                itemView -> {
                    onClickItem.invoke(it)
                }

                itemView.tvState -> {
                    // TODO 撤销操作
                }
            }
        }
    }
}