package com.violas.wallet.ui.record

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.violas.wallet.R
import com.violas.wallet.base.BaseViewHolder
import com.violas.wallet.base.paging.PagingViewAdapter
import com.violas.wallet.getColor
import com.violas.wallet.utils.convertAmountToDisplayUnit
import kotlinx.android.synthetic.main.item_transaction_record.view.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2019-11-07 11:45.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易记录的ViewAdapter
 */
class TransactionRecordViewAdapter(
    retryCallback: () -> Unit,
    private val onClickQuery: (TransactionRecordVO) -> Unit
) :
    PagingViewAdapter<TransactionRecordVO>(retryCallback, TransactionRecordDiffCallback()) {

    private val mSimpleDateFormat = SimpleDateFormat("yy.MM.dd HH:mm", Locale.CHINA)

    override fun onCreateViewHolderSupport(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<TransactionRecordVO> {
        return TransactionRecordViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_transaction_record,
                parent,
                false
            ),
            mSimpleDateFormat,
            onClickQuery
        )
    }
}

class TransactionRecordViewHolder(
    view: View,
    private val mSimpleDateFormat: SimpleDateFormat,
    private val onClickQuery: (TransactionRecordVO) -> Unit
) :
    BaseViewHolder<TransactionRecordVO>(view) {

    init {
        itemView.vQuery.setOnClickListener(this)
    }

    override fun onViewBind(itemIndex: Int, itemDate: TransactionRecordVO?) {
        itemDate?.let {
            itemView.vTime.text = mSimpleDateFormat.format(it.time)
            val displayUnit = convertAmountToDisplayUnit(it.amount, it.coinTypes)
            itemView.vAmount.text = "${displayUnit.first} ${displayUnit.second}"
            itemView.vCoinName.text = it.coinTypes.coinName()
            itemView.vAddress.text = it.address
            when (it.transactionType) {
                TransactionRecordVO.TRANSACTION_TYPE_RECEIPT -> {
                    itemView.vType.setText(R.string.transaction_record_receipt)
                    itemView.vType.setTextColor(getColor(R.color.color_13B788))
                }

                else -> {
                    itemView.vType.setText(R.string.transaction_record_payment)
                    itemView.vType.setTextColor(getColor(R.color.color_E54040))
                }
            }
        }
    }

    override fun onViewClick(view: View, itemIndex: Int, itemDate: TransactionRecordVO?) {
        itemDate?.let {
            when (view) {
                itemView.vQuery -> {
                    onClickQuery.invoke(it)
                }
            }
        }
    }
}

class TransactionRecordDiffCallback : DiffUtil.ItemCallback<TransactionRecordVO>() {
    override fun areItemsTheSame(
        oldItem: TransactionRecordVO,
        newItem: TransactionRecordVO
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: TransactionRecordVO,
        newItem: TransactionRecordVO
    ): Boolean {
        return oldItem == newItem
    }
}