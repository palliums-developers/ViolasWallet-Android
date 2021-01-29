package com.violas.wallet.ui.bank.order.borrowing

import android.content.Context
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
import com.violas.wallet.event.BankRepaymentEvent
import com.violas.wallet.repository.http.bank.BorrowingInfoDTO
import com.violas.wallet.ui.bank.details.borrowing.BankBorrowingDetailsActivity
import com.violas.wallet.ui.bank.order.BaseBankOrderActivity
import com.violas.wallet.ui.bank.record.borrowing.BankBorrowingRecordActivity
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.loadCircleImage
import kotlinx.android.synthetic.main.activity_bank_order.*
import kotlinx.android.synthetic.main.item_bank_curr_borrowing.view.*
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * Created by elephant on 2020/8/24 18:09.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行借款订单页面
 */
class BankBorrowingOrderActivity : BaseBankOrderActivity<BorrowingInfoDTO>() {

    companion object {

        fun start(context: Context) {
            Intent(context, BankBorrowingOrderActivity::class.java).start(context)
        }
    }

    override fun lazyInitPagingViewModel(): PagingViewModel<BorrowingInfoDTO> {
        return ViewModelProvider(this).get(BankBorrowingOrderViewModel::class.java)
    }

    override fun lazyInitPagingViewAdapter(): PagingViewAdapter<BorrowingInfoDTO> {
        return ViewAdapter { borrowingInfo, position ->
            BankBorrowingDetailsActivity.start(this, borrowingInfo)
        }
    }

    private fun getViewModel(): BankBorrowingOrderViewModel {
        return getPagingViewModel() as BankBorrowingOrderViewModel
    }

    override fun onTitleRightViewClick() {
        BankBorrowingRecordActivity.start(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.bank_borrowing_orders_title)
        ivIcon.setImageResource(getResourceId(R.attr.bankBorrowingOrderIcon, this))
        tvLabel.setText(R.string.bank_borrowing_orders_label_current_borrowing)

        launch {
            if (getViewModel().initAddress()) {
                EventBus.getDefault().register(this@BankBorrowingOrderActivity)
                getPagingHandler().start()
            } else {
                statusLayout.showStatus(IStatusLayout.Status.STATUS_EMPTY)
            }
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe
    fun onBankRepaymentEvent(event: BankRepaymentEvent) {
        launch {
            getRefreshLayout()?.autoRefresh()
        }
    }

    class ViewAdapter(
        private val itemClickCallback: (BorrowingInfoDTO, Int) -> Unit
    ) : PagingViewAdapter<BorrowingInfoDTO>() {

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
        private val itemClickCallback: (BorrowingInfoDTO, Int) -> Unit
    ) : BaseViewHolder<BorrowingInfoDTO>(view) {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: BorrowingInfoDTO?) {
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
            itemData: BorrowingInfoDTO?
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