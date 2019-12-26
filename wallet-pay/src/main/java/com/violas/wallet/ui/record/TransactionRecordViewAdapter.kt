package com.violas.wallet.ui.record

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.palliums.base.BaseViewHolder
import com.palliums.utils.getColor
import com.palliums.utils.getString
import com.violas.wallet.R
import com.palliums.paging.PagingViewAdapter
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

    private val mSimpleDateFormat = SimpleDateFormat("yy.MM.dd HH:mm", Locale.ENGLISH)

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

    override fun onViewBind(itemPosition: Int, itemData: TransactionRecordVO?) {
        itemData?.let {
            itemView.vTime.text = mSimpleDateFormat.format(it.time)
            itemView.vAddress.text = it.address

            if (TransactionRecordVO.isOpenToken(it.transactionType)) {
                val amountInfo = convertAmountToDisplayUnit(it.gas, it.coinTypes)

                itemView.vAmountLabel.setText(R.string.transaction_record_consume)
                itemView.vAmount.text = "${amountInfo.first} ${amountInfo.second}"

                itemView.vType.setText(R.string.transaction_record_open)
                itemView.vType.setTextColor(getColor(R.color.color_FB8F0B))
                itemView.vCoinName.text =
                    it.coinName ?: getString(R.string.transaction_record_new_coin)
            } else {
                val amountInfo = convertAmountToDisplayUnit(it.amount, it.coinTypes)

                itemView.vAmountLabel.setText(R.string.transaction_record_amount)
                itemView.vAmount.text = amountInfo.first

                when (it.transactionType) {
                    TransactionRecordVO.TRANSACTION_TYPE_TOKEN_RECEIPT -> {
                        itemView.vType.setText(R.string.transaction_record_receipt)
                        itemView.vType.setTextColor(getColor(R.color.color_13B788))
                        itemView.vCoinName.text = it.coinName ?: ""
                    }

                    TransactionRecordVO.TRANSACTION_TYPE_TOKEN_TRANSFER -> {
                        itemView.vType.setText(R.string.transaction_record_transfer)
                        itemView.vType.setTextColor(getColor(R.color.color_E54040))
                        itemView.vCoinName.text = it.coinName ?: ""
                    }

                    TransactionRecordVO.TRANSACTION_TYPE_RECEIPT -> {
                        itemView.vType.setText(R.string.transaction_record_receipt)
                        itemView.vType.setTextColor(getColor(R.color.color_13B788))
                        itemView.vCoinName.text = it.coinTypes.coinUnit()
                    }

                    else -> {
                        itemView.vType.setText(R.string.transaction_record_transfer)
                        itemView.vType.setTextColor(getColor(R.color.color_E54040))
                        itemView.vCoinName.text = it.coinTypes.coinUnit()
                    }
                }
            }
        }
    }

    override fun onViewClick(view: View, itemPosition: Int, itemData: TransactionRecordVO?) {
        itemData?.let {
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