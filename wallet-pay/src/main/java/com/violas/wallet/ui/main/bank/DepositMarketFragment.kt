package com.violas.wallet.ui.main.bank

import android.view.View
import com.palliums.utils.getResourceId
import com.violas.wallet.R
import com.violas.wallet.repository.http.bank.BankProductSummaryDTO
import com.violas.wallet.utils.keepTwoDecimals
import com.violas.wallet.utils.loadCircleImage
import kotlinx.android.synthetic.main.item_home_bank_product.view.*

/**
 * Created by elephant on 2020/8/31 14:59.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 首页-银行-存款市场
 */
class DepositMarketFragment : BaseBankMarketFragment<BankProductSummaryDTO>() {

    override fun onBindView(itemData: BankProductSummaryDTO, itemView: View) {
        itemView.ivLogo.loadCircleImage(
            itemData.productLogo,
            getResourceId(R.attr.iconCoinDefLogo, itemView.context)
        )
        itemView.tvName.text = itemData.productName
        itemView.tvDesc.text = itemData.productDesc
        itemView.tvRate.text = "${keepTwoDecimals(itemData.productRate)}%"
        itemView.tvRateLabel.setText(R.string.deposit_yield)
    }

    override fun onItemClick(itemData: BankProductSummaryDTO, itemPosition: Int) {

    }
}