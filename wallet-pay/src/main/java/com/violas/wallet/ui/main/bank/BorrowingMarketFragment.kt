package com.violas.wallet.ui.main.bank

import android.view.View
import com.palliums.utils.getResourceId
import com.violas.wallet.R
import com.violas.wallet.repository.http.bank.BorrowingProductSummaryDTO
import com.violas.wallet.ui.bank.borrow.BorrowActivity
import com.violas.wallet.utils.convertRateToPercentage
import com.violas.wallet.utils.loadCircleImage
import kotlinx.android.synthetic.main.item_home_bank_product.view.*

/**
 * Created by elephant on 2020/8/31 14:59.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 首页-银行-借款市场
 */
class BorrowingMarketFragment : BaseBankMarketFragment<BorrowingProductSummaryDTO>() {

    override fun onBindView(itemData: BorrowingProductSummaryDTO, itemView: View) {
        itemView.ivLogo.loadCircleImage(
            itemData.productLogo,
            getResourceId(R.attr.iconCoinDefLogo, itemView.context)
        )
        itemView.tvName.text = itemData.productName
        itemView.tvDesc.text = itemData.productDesc
        itemView.tvRate.text = convertRateToPercentage(itemData.borrowingRate)
        itemView.tvRateLabel.setText(R.string.borrowing_rate)
    }

    override fun onItemClick(itemData: BorrowingProductSummaryDTO, itemPosition: Int) {
        BorrowActivity.start(requireContext(), itemData.productId)
    }
}