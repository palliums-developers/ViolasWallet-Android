package com.violas.wallet.ui.dexOrder.details

import android.view.View
import com.palliums.base.BaseViewHolder
import com.palliums.utils.formatDate
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.repository.http.dex.DexOrderDTO
import com.violas.wallet.utils.convertViolasTokenPrice
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
    private val onClickRevokeOrder: ((order: DexOrderDTO, position: Int) -> Unit)? = null
) : BaseViewHolder<DexOrderDTO>(view) {

    init {
        itemView.tvState.setOnClickListener(this)
        itemView.tvBrowserQuery.setOnClickListener(this)
    }

    override fun onViewBind(itemPosition: Int, itemData: DexOrderDTO?) {
        itemData?.let {
            // 若拿A换B，A在前B在后
            itemView.tvGiveTokenName.text = "${it.tokenGiveSymbol} /"
            itemView.tvGetTokenName.text = it.tokenGetSymbol

            // 若拿A换B，价格、数量、已成交数量均为B的数据
            itemView.tvPrice.text = convertViolasTokenPrice(it.tokenGetPrice.toString())
            itemView.tvTotalLiquidityAmount.text = convertViolasTokenUnit(it.amountGet)
            itemView.tvTradeAmount.text = convertViolasTokenUnit(it.amountFilled)

            itemView.tvFee.text = "0 ${CoinTypes.Violas.coinUnit()}"
            itemView.tvTime.text = formatDate(it.updateDate, simpleDateFormat)

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

    override fun onViewClick(view: View, itemPosition: Int, itemData: DexOrderDTO?) {
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