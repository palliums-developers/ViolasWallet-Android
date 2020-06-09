package com.violas.wallet.ui.transactionRecord

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
    private val onItemClick: (TransactionRecordVO) -> Unit
) :
    PagingViewAdapter<TransactionRecordVO>(retryCallback, TransactionRecordDiffCallback()) {

    private val mSimpleDateFormat = SimpleDateFormat("MM/dd HH:mm:ss", Locale.ENGLISH)

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
            onItemClick
        )
    }
}

class TransactionRecordViewHolder(
    view: View,
    private val mSimpleDateFormat: SimpleDateFormat,
    private val onItemClick: (TransactionRecordVO) -> Unit
) :
    BaseViewHolder<TransactionRecordVO>(view) {

    init {
        itemView.setOnClickListener(this)
    }

    override fun onViewBind(itemPosition: Int, itemData: TransactionRecordVO?) {
        itemData?.let {

            itemView.ivType.setImageResource(
                when (it.transactionType) {
                    TransactionType.COLLECTION ->
                        R.drawable.ic_transaction_type_collection

                    TransactionType.TRANSFER ->
                        R.drawable.ic_transaction_type_transfer

                    else ->
                        R.drawable.ic_transaction_type_register
                }
            )

            itemView.tvAddress.text = if (it.fromAddress.length > 20)
                "${it.fromAddress.substring(0, 10)}...${it.fromAddress.substring(
                    it.fromAddress.length - 10,
                    it.fromAddress.length
                )}"
            else
                it.fromAddress

            itemView.tvTime.text = mSimpleDateFormat.format(it.time)

            itemView.tvAmount.text = convertAmountToDisplayUnit(it.amount, it.coinType).first
            itemView.tvAmount.setTextColor(
                getColor(
                    when (it.transactionState) {
                        TransactionState.SUCCESS -> {
                            itemView.tvDesc.visibility = View.GONE
                            R.color.color_13B788
                        }

                        TransactionState.FAILURE -> {
                            itemView.tvDesc.visibility = View.GONE
                            R.color.color_E54040
                        }

                        else -> {
                            itemView.tvDesc.visibility = View.VISIBLE
                            itemView.tvDesc.text = getString(R.string.state_trading)
                            itemView.tvDesc.setTextColor(getColor(R.color.color_FB8F0B))
                            R.color.color_FB8F0B
                        }
                    }
                )
            )
        }
    }

    override fun onViewClick(view: View, itemPosition: Int, itemData: TransactionRecordVO?) {
        itemData?.let {
            when (view) {
                itemView -> {
                    onItemClick.invoke(it)
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