package com.violas.wallet.ui.bank.order.borrowing

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.listing.ListingViewAdapter
import com.palliums.listing.ListingViewModel
import com.palliums.utils.getResourceId
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.repository.http.bank.CurrBorrowingDTO
import com.violas.wallet.ui.bank.details.borrowing.BankBorrowingDetailsActivity
import com.violas.wallet.ui.bank.order.BaseBankOrderActivity
import com.violas.wallet.ui.bank.record.borrowing.BankBorrowingRecordActivity
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.loadCircleImage
import kotlinx.android.synthetic.main.activity_bank_order.*
import kotlinx.android.synthetic.main.item_bank_curr_borrowing.view.*
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2020/8/24 18:09.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行借款订单页面
 */
class BankBorrowingOrderActivity : BaseBankOrderActivity<CurrBorrowingDTO>() {

    private val viewModel by lazy {
        ViewModelProvider(this).get(BankBorrowingOrderViewModel::class.java)
    }
    private val viewAdapter by lazy {
        CurrBorrowingViewAdapter { currBorrowing, position ->
            BankBorrowingDetailsActivity.start(this, currBorrowing)
        }
    }

    override fun getViewModel(): ListingViewModel<CurrBorrowingDTO> {
        return viewModel
    }

    override fun getViewAdapter(): ListingViewAdapter<CurrBorrowingDTO> {
        return viewAdapter
    }

    override fun onTitleRightViewClick() {
        Intent(this, BankBorrowingRecordActivity::class.java).start(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.borrowing_order)
        ivIcon.setImageResource(getResourceId(R.attr.bankBorrowingOrderIcon, this))
        tvLabel.setText(R.string.current_borrowing)

        launch {
            val initResult = viewModel.initAddress()
            if (!initResult) {
                close()
                return@launch
            }

            viewModel.execute()
        }
    }

    class CurrBorrowingViewAdapter(
        private val itemClickCallback: (CurrBorrowingDTO, Int) -> Unit
    ) : ListingViewAdapter<CurrBorrowingDTO>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<CurrBorrowingDTO> {
            return CurrBorrowingViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_bank_curr_borrowing, parent, false
                ),
                itemClickCallback
            )
        }
    }

    class CurrBorrowingViewHolder(
        view: View,
        private val itemClickCallback: (CurrBorrowingDTO, Int) -> Unit
    ) : BaseViewHolder<CurrBorrowingDTO>(view) {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: CurrBorrowingDTO?) {
            itemData?.let {
                itemView.ivCoinLogo.loadCircleImage(
                    it.coinLogo,
                    getResourceId(R.attr.iconCoinDefLogo, itemView.context)
                )
                itemView.tvCoinName.text = it.coinName
                itemView.tvAmountToBeRepaid.text = convertAmountToDisplayAmountStr(it.borrowed)
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: CurrBorrowingDTO?) {
            itemData?.let {
                when (view) {
                    itemView -> {
                        itemClickCallback.invoke(it, itemPosition)
                    }
                }
            }
        }
    }
}