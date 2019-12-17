package com.violas.wallet.ui.dexOrder

import android.view.View
import com.palliums.base.BaseViewHolder
import com.palliums.utils.formatDate
import com.palliums.utils.getColor
import com.violas.wallet.R
import com.violas.wallet.utils.convertViolasTokenUnit
import kotlinx.android.synthetic.main.item_dex_order.view.*
import java.text.SimpleDateFormat

/**
 * Created by elephant on 2019-12-09 16:53.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DexOrderViewHolder(
    view: View,
    private val simpleDateFormat: SimpleDateFormat,
    private val onClickItem: ((DexOrderVO) -> Unit)? = null,
    private val onClickRevokeOrder: ((DexOrderVO, Int) -> Unit)? = null
) : BaseViewHolder<DexOrderVO>(view) {

    init {
        itemView.setOnClickListener(this)
        itemView.tvState.setOnClickListener(this)
    }

    override fun onViewBind(itemIndex: Int, itemData: DexOrderVO?) {
        itemData?.let {
            // 若拿A换B，A在前B在后
            itemView.tvGiveTokenName.text = "${it.giveTokenName} /"
            itemView.tvGetTokenName.text = it.getTokenName

            // 若拿A换B，价格、数量、已成交数量均为B的数据
            itemView.tvPrice.text = it.getTokenPrice.toString()
            itemView.tvTotalAmount.text = convertViolasTokenUnit(it.dto.amountGet)
            itemView.tvTradeAmount.text = convertViolasTokenUnit(it.dto.amountFilled)

            itemView.tvFee.text = "0.00Vtoken"

            when {
                it.isFinished() -> {
                    itemView.tvState.setText(
                        if (it.isCanceled())
                            R.string.state_revoked
                        else
                            R.string.state_completed
                    )
                    itemView.tvState.setTextColor(getColor(R.color.color_63636F, itemView.context))
                    itemView.tvTime.text = formatDate(it.dto.updateDate, simpleDateFormat)
                }
                it.isOpen() -> {
                    itemView.tvState.setText(
                        if (it.revokedFlag)
                            R.string.state_revoked
                        else
                            R.string.action_revoke
                    )
                    itemView.tvState.setTextColor(getColor(R.color.color_726BD9, itemView.context))
                    itemView.tvTime.text = formatDate(it.dto.date, simpleDateFormat)
                }
                else -> {
                    itemView.tvState.text = ""
                    itemView.tvTime.text = formatDate(it.dto.updateDate, simpleDateFormat)
                }
            }
        }
    }

    override fun onViewClick(view: View, itemIndex: Int, itemData: DexOrderVO?) {
        itemData?.let {
            when (view) {
                itemView -> {
                    onClickItem?.invoke(it)
                }

                itemView.tvState -> {
                    if (it.isOpen() && !it.revokedFlag) {
                        onClickRevokeOrder?.invoke(itemData, itemIndex)
                    } else {
                    }
                }

                else -> {
                }
            }
        }
    }
}