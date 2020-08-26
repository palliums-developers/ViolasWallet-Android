package com.violas.wallet.ui.bank.record.deposit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.formatDate
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.getResourceId
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.repository.http.bank.DepositRecordDTO
import com.violas.wallet.ui.bank.record.BaseBankRecordActivity
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.loadCircleImage
import kotlinx.android.synthetic.main.item_bank_transaction_record.view.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2020/8/25 16:39.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行存款记录页面
 */
class BankDepositRecordActivity : BaseBankRecordActivity<DepositRecordDTO>() {

    private val viewModel by lazy {
        ViewModelProvider(this).get(BankDepositRecordViewModel::class.java)
    }
    private val viewAdapter by lazy {
        ViewAdapter {
            viewModel.retry()
        }
    }

    override fun getViewModel(): PagingViewModel<DepositRecordDTO> {
        return viewModel
    }

    override fun getViewAdapter(): PagingViewAdapter<DepositRecordDTO> {
        return viewAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.deposit_details)
        mPagingHandler.start()
    }

    override fun getCurrFilterLiveData(coinFilter: Boolean): MutableLiveData<Pair<Int, String?>> {
        return if (coinFilter)
            viewModel.currCoinFilterLiveData
        else
            viewModel.currStateFilterLiveData
    }

    override fun getFilterData(coinFilter: Boolean): MutableList<String> {
        return if (coinFilter)
            mutableListOf(
                getString(R.string.all_currencies),
                "VLS",
                "BTC",
                "USD",
                "EUR",
                "GBP",
                "SGD",
                "VLSUSD",
                "VLSEUR",
                "VLSGBP",
                "VLSSGD"
            )
        else
            mutableListOf(
                getString(R.string.label_all),
                getString(R.string.bank_deposit_state_deposited),
                getString(R.string.bank_deposit_state_withdrew)
            )
    }

    class ViewAdapter(
        retryCallback: () -> Unit
    ) : PagingViewAdapter<DepositRecordDTO>(retryCallback, DiffCallback()) {

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
    ) : BaseViewHolder<DepositRecordDTO>(view) {

        override fun onViewBind(itemPosition: Int, itemData: DepositRecordDTO?) {
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
                        0 -> R.string.bank_deposit_state_depositing
                        1 -> R.string.bank_deposit_state_deposited
                        2 -> R.string.bank_deposit_state_withdrew
                        3 -> R.string.bank_deposit_state_withdrawing
                        4 -> R.string.bank_deposit_state_deposit_failed
                        else -> R.string.bank_deposit_state_withdrawal_failed
                    }
                )
                itemView.tvDesc.setTextColor(
                    getColorByAttrId(
                        when (it.state) {
                            0, 3 -> R.attr.textColorProcessing
                            else -> android.R.attr.textColorTertiary
                        },
                        itemView.context
                    )
                )
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DepositRecordDTO>() {
        override fun areItemsTheSame(
            oldItem: DepositRecordDTO,
            newItem: DepositRecordDTO
        ): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(
            oldItem: DepositRecordDTO,
            newItem: DepositRecordDTO
        ): Boolean {
            return oldItem == newItem
        }
    }
}