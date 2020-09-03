package com.violas.wallet.ui.bank.record.borrowing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.extensions.getShowErrorMessage
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.*
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.repository.http.bank.BorrowingRecordDTO
import com.violas.wallet.ui.bank.record.BaseBankRecordActivity
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.loadCircleImage
import kotlinx.android.synthetic.main.activity_bank_record.*
import kotlinx.android.synthetic.main.item_bank_transaction_record.view.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2020/8/25 16:39.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行借款记录页面
 */
class BankBorrowingRecordActivity : BaseBankRecordActivity<BorrowingRecordDTO>() {

    companion object {

        fun start(context: Context) {
            Intent(context, BankBorrowingRecordActivity::class.java).start(context)
        }
    }

    private val viewModel by lazy {
        ViewModelProvider(this).get(BankBorrowingRecordViewModel::class.java)
    }
    private val viewAdapter by lazy {
        ViewAdapter()
    }

    override fun getViewModel(): PagingViewModel<BorrowingRecordDTO> {
        return viewModel
    }

    override fun getViewAdapter(): PagingViewAdapter<BorrowingRecordDTO> {
        return viewAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.borrowing_record)
        launch {
            if (viewModel.initAddress()) {
                mPagingHandler.start()
            } else {
                statusLayout.showStatus(IStatusLayout.Status.STATUS_EMPTY)
            }
        }
    }

    override fun getCurrFilterLiveData(coinFilter: Boolean): MutableLiveData<Pair<Int, String>?> {
        return if (coinFilter)
            viewModel.currCoinFilterLiveData
        else
            viewModel.currStateFilterLiveData
    }

    override fun getFilterDataLiveData(coinFilter: Boolean): MutableLiveData<MutableList<String>> {
        return if (coinFilter)
            viewModel.coinFilterDataLiveData
        else
            viewModel.stateFilterDataLiveData
    }

    override fun loadFilterData(coinFilter: Boolean) {
        if (coinFilter) {
            launch {
                showProgress()
                try {
                    viewModel.loadCoinFilterData()
                } catch (e: Exception) {
                    showToast(e.getShowErrorMessage(true))
                }
                dismissProgress()
            }
        } else {
            viewModel.loadStateFilterData()
        }
    }

    class ViewAdapter : PagingViewAdapter<BorrowingRecordDTO>() {

        private val simpleDateFormat by lazy {
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH)
        }

        override fun onCreateViewHolderSupport(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<out Any> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_bank_transaction_record,
                    parent,
                    false
                ),
                simpleDateFormat
            )
        }
    }

    class ViewHolder(
        view: View,
        private val simpleDateFormat: SimpleDateFormat
    ) : BaseViewHolder<BorrowingRecordDTO>(view) {

        override fun onViewBind(itemPosition: Int, itemData: BorrowingRecordDTO?) {
            itemData?.let {
                itemView.ivCoinLogo.loadCircleImage(
                    it.coinLogo,
                    getResourceId(R.attr.iconCoinDefLogo, itemView.context)
                )
                itemView.tvCoinName.text = it.coinName
                itemView.tvTime.text = formatDate(it.time, simpleDateFormat)
                itemView.tvAmount.text = convertAmountToDisplayAmountStr(it.amount)
                itemView.tvDesc.text = getString(
                    when (it.state) {
                        0 -> R.string.bank_borrowing_state_borrowing
                        1 -> R.string.bank_borrowing_state_borrowed
                        2 -> R.string.bank_borrowing_state_repaid
                        3 -> R.string.bank_borrowing_state_liquidated
                        else -> R.string.bank_borrowing_state_repaying
                    }
                )
                itemView.tvDesc.setTextColor(
                    getColorByAttrId(
                        when (it.state) {
                            0, 4 -> R.attr.textColorProcessing
                            else -> android.R.attr.textColorTertiary
                        },
                        itemView.context
                    )
                )
            }
        }
    }
}