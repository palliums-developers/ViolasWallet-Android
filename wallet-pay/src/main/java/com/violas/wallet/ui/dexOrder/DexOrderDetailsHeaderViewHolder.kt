package com.violas.wallet.ui.dexOrder

import android.view.View
import com.palliums.base.BaseViewHolder
import com.violas.wallet.R
import kotlinx.android.synthetic.main.item_dex_order_details_header.view.*
import java.text.SimpleDateFormat

/**
 * Created by elephant on 2019-12-10 15:45.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DexOrderDetailsHeaderViewHolder(
    view: View,
    private val simpleDateFormat: SimpleDateFormat,
    private val onClickBrowserQuery: ((DexOrderVO) -> Unit)? = null,
    private val onClickRevokeOrder: ((DexOrderVO, Int) -> Unit)? = null
) : BaseViewHolder<DexOrderVO>(view) {

    init {
        itemView.tvState.setOnClickListener(this)
        itemView.tvBrowserQuery.setOnClickListener(this)
    }

    override fun onViewBind(itemIndex: Int, itemData: DexOrderVO?) {
        itemData?.let {
            // 若拿A换B，A在前B在后
            itemView.tvGiveTokenName.text = "${it.giveTokenName} /"
            itemView.tvGetTokenName.text = it.getTokenName

            // 若拿A换B，价格、数量、已成交数量均为B的数据
            itemView.tvPrice.text = it.getTokenPrice.toString()
            itemView.tvTotalAmount.text = it.dexOrderDTO.amountGet
            itemView.tvTradeAmount.text = it.dexOrderDTO.amountFilled

            itemView.tvFee.text = "0.00Vtoken"

            when {
                it.isFinished() -> {
                    itemView.tvState.setText(
                        if (it.isCanceled())
                            R.string.state_revoked
                        else
                            R.string.state_completed
                    )
                    itemView.tvTime.text = simpleDateFormat.format(it.getUpdateDate())
                }
                it.isOpen() -> {
                    itemView.tvState.setText(
                        if (it.revokedFlag)
                            R.string.state_revoked
                        else
                            R.string.action_revoke
                    )
                    itemView.tvTime.text = simpleDateFormat.format(it.getDate())
                }
                else -> {
                    itemView.tvState.text = ""
                    itemView.tvTime.text = simpleDateFormat.format(it.getUpdateDate())
                }
            }
        }
    }

    override fun onViewClick(view: View, itemIndex: Int, itemData: DexOrderVO?) {
        itemData?.let {
            when (view) {
                itemView.tvState -> {
                    if (it.isOpen() && !it.revokedFlag) {
                        onClickRevokeOrder?.invoke(itemData, itemIndex)
                    } else {
                    }
                }

                itemView.tvBrowserQuery -> {
                    onClickBrowserQuery?.invoke(it)
                }

                else -> {
                }
            }
        }
    }
}