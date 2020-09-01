package com.violas.wallet.ui.bank.order.borrowing

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.getResourceId
import com.palliums.utils.start
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.repository.http.bank.AccountBorrowingInfoDTO
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
class BankBorrowingOrderActivity : BaseBankOrderActivity<AccountBorrowingInfoDTO>() {

    private val viewModel by lazy {
        ViewModelProvider(this).get(BankBorrowingOrderViewModel::class.java)
    }
    private val viewAdapter by lazy {
        ViewAdapter { borrowingInfo, position ->
            BankBorrowingDetailsActivity.start(this, borrowingInfo)
        }
    }

    override fun getViewModel(): PagingViewModel<AccountBorrowingInfoDTO> {
        return viewModel
    }

    override fun getViewAdapter(): PagingViewAdapter<AccountBorrowingInfoDTO> {
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
            val hasAccount = viewModel.initAddress()
            if (hasAccount) {
                viewModel.start()
            } else {
                refreshLayout.setEnableRefresh(false)
                statusLayout.showStatus(IStatusLayout.Status.STATUS_EMPTY)
            }
        }
    }

    class ViewAdapter(
        private val itemClickCallback: (AccountBorrowingInfoDTO, Int) -> Unit
    ) : PagingViewAdapter<AccountBorrowingInfoDTO>() {

        override fun onCreateViewHolderSupport(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<out Any> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_bank_curr_borrowing, parent, false
                ),
                itemClickCallback
            )
        }
    }

    class ViewHolder(
        view: View,
        private val itemClickCallback: (AccountBorrowingInfoDTO, Int) -> Unit
    ) : BaseViewHolder<AccountBorrowingInfoDTO>(view) {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: AccountBorrowingInfoDTO?) {
            itemData?.let {
                itemView.ivCoinLogo.loadCircleImage(
                    it.productLogo,
                    getResourceId(R.attr.iconCoinDefLogo, itemView.context)
                )
                itemView.tvCoinName.text = it.productName
                itemView.tvAmountToBeRepaid.text =
                    convertAmountToDisplayAmountStr(it.borrowedAmount)
            }
        }

        override fun onViewClick(
            view: View,
            itemPosition: Int,
            itemData: AccountBorrowingInfoDTO?
        ) {
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