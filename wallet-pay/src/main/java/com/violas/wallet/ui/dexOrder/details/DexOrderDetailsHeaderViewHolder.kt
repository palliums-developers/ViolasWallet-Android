package com.violas.wallet.ui.dexOrder.details

import android.view.View
import com.palliums.base.BaseViewHolder
import com.palliums.utils.formatDate
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.ui.dexOrder.DexOrderVO
import com.violas.wallet.utils.convertViolasTokenUnit
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
    private val onClickBrowserQuery: ((url: String?) -> Unit)? = null,
    private val onClickRevokeOrder: ((order: DexOrderVO, position: Int) -> Unit)? = null
) : BaseViewHolder<DexOrderVO>(view) {

    init {
        itemView.tvState.setOnClickListener(this)
        itemView.tvBrowserQuery.setOnClickListener(this)
    }

    override fun onViewBind(itemPosition: Int, itemData: DexOrderVO?) {
        itemData?.let {
            // 若拿A换B，A在前B在后
            itemView.tvGiveTokenName.text = "${it.giveTokenName} /"
            itemView.tvGetTokenName.text = it.getTokenName

            // 若拿A换B，价格、数量、已成交数量均为B的数据
            itemView.tvPrice.text = it.getTokenPrice.toString()
            itemView.tvTotalAmount.text = convertViolasTokenUnit(it.dto.amountGet)
            itemView.tvTradeAmount.text = convertViolasTokenUnit(it.dto.amountFilled)

            itemView.tvFee.text = "0.00${CoinTypes.Violas.coinUnit()}"
            itemView.tvTime.text = formatDate(it.dto.updateDate, simpleDateFormat)

            when {
                it.isFinished() -> {
                    itemView.tvState.setText(
                        if (it.isCanceled())
                            R.string.state_revoked
                        else
                            R.string.state_completed
                    )
                }
                it.isUnfinished() -> {
                    itemView.tvState.setText(
                        if (it.isOpen())
                            R.string.action_revoke
                        else
                            R.string.state_revoking
                    )
                }
                else -> {
                    itemView.tvState.text = ""
                }
            }
        }
    }

    override fun onViewClick(view: View, itemPosition: Int, itemData: DexOrderVO?) {
        itemData?.let {
            when (view) {
                itemView.tvState -> {
                    if (it.isOpen()) {
                        onClickRevokeOrder?.invoke(it, itemPosition)
                    } else {
                    }
                }

                itemView.tvBrowserQuery -> {
                    onClickBrowserQuery?.invoke(null)
                }

                else -> {
                }
            }
        }
    }
}