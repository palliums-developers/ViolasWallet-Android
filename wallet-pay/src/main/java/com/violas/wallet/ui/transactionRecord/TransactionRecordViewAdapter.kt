package com.violas.wallet.ui.transactionRecord

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.palliums.utils.formatDateWithNotNeedCorrectDate
import com.palliums.utils.getColor
import com.palliums.utils.getResourceId
import com.violas.wallet.R
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.utils.getAmountPrefix
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
    private val onItemClick: (TransactionRecordVO) -> Unit
) : PagingViewAdapter<TransactionRecordVO>() {

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
                getResourceId(
                    when (it.transactionType) {
                        TransactionType.COLLECTION ->
                            R.attr.iconRecordTypeInput

                        TransactionType.ADD_CURRENCY ->
                            R.attr.iconRecordTypeAddCurrency

                        else ->
                            R.attr.iconRecordTypeOutput
                    },
                    itemView.context
                )
            )

            val showAddress = if (it.transactionType == TransactionType.COLLECTION) {
                it.fromAddress
            } else {
                it.toAddress ?: it.fromAddress
            }
            itemView.tvAddress.text = if (showAddress.length > 12)
                "${showAddress.substring(0, 6)}...${
                    showAddress.substring(
                        showAddress.length - 6,
                        showAddress.length
                    )
                }"
            else
                showAddress

            itemView.tvTime.text = formatDateWithNotNeedCorrectDate(it.time, mSimpleDateFormat)

            val amount = it.amount.toBigDecimalOrNull()
            if (amount != null) {
                val amountPrefix =
                    getAmountPrefix(amount, it.transactionType == TransactionType.COLLECTION)
                val amountWithUnit = convertAmountToDisplayUnit(amount, it.coinType)
                itemView.tvAmount.text = "$amountPrefix${amountWithUnit.first}"
            } else {
                itemView.tvAmount.setText(R.string.common_desc_value_null)
            }

            itemView.tvAmount.setTextColor(
                getColor(
                    getResourceId(
                        when (it.transactionState) {
                            TransactionState.SUCCESS -> {
                                itemView.tvDesc.visibility = View.GONE
                                R.attr.textColorSuccess
                            }

                            TransactionState.FAILURE -> {
                                itemView.tvDesc.visibility = View.GONE
                                R.attr.textColorFailure
                            }

                            else -> {
                                itemView.tvDesc.visibility = View.VISIBLE
                                itemView.tvDesc.setText(R.string.txn_details_state_processing)
                                itemView.tvDesc.setTextColor(
                                    getColor(
                                        getResourceId(
                                            R.attr.textColorProcessing,
                                            itemView.context
                                        ),
                                        itemView.context
                                    )
                                )
                                R.attr.textColorProcessing
                            }
                        },
                        itemView.context
                    ),
                    itemView.context
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