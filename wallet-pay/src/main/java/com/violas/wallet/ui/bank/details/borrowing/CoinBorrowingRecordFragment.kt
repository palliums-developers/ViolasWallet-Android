package com.violas.wallet.ui.bank.details.borrowing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.formatDate
import com.palliums.utils.getColorByAttrId
import com.palliums.widget.refresh.IRefreshLayout
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.common.KEY_THREE
import com.violas.wallet.common.KEY_TWO
import com.violas.wallet.repository.http.bank.CoinBorrowingRecordDTO
import com.violas.wallet.ui.bank.details.BaseCoinTxRecordFragment
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import kotlinx.android.synthetic.main.fragment_bank_coin_borrowing_record.*
import kotlinx.android.synthetic.main.item_bank_coin_borrowing_record.view.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2020/8/28 15:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 币种借款记录视图
 */
class CoinBorrowingRecordFragment : BaseCoinTxRecordFragment<CoinBorrowingRecordDTO>() {

    companion object {
        fun newInstance(
            productId: String,
            walletAddress: String,
            currency: String
        ): CoinBorrowingRecordFragment {
            return CoinBorrowingRecordFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_ONE, productId)
                    putString(KEY_TWO, walletAddress)
                    putString(KEY_THREE, currency)
                }
            }
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_bank_coin_borrowing_record
    }

    override fun getRecyclerView(): RecyclerView {
        return vRecyclerView
    }

    override fun getRefreshLayout(): IRefreshLayout? {
        return vRefreshLayout
    }

    override fun getStatusLayout(): IStatusLayout? {
        return vStatusLayout
    }

    override fun lazyInitPagingViewModel(): PagingViewModel<CoinBorrowingRecordDTO> {
        return ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    return modelClass
                        .getConstructor(String::class.java, String::class.java)
                        .newInstance(mProductId, mWalletAddress)
                }
            }
        ).get(CoinBorrowingRecordViewModel::class.java)
    }

    override fun lazyInitPagingViewAdapter(): PagingViewAdapter<CoinBorrowingRecordDTO> {
        return ViewAdapter()
    }

    class ViewAdapter : PagingViewAdapter<CoinBorrowingRecordDTO>() {

        private val simpleDateFormat by lazy {
            SimpleDateFormat("HH:mm:ss MM/dd", Locale.ENGLISH)
        }

        override fun onCreateViewHolderSupport(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<out Any> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_bank_coin_borrowing_record,
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
    ) : BaseViewHolder<CoinBorrowingRecordDTO>(view) {

        override fun onViewBind(itemPosition: Int, itemData: CoinBorrowingRecordDTO?) {
            itemData?.let {
                itemView.tvTime.text = formatDate(it.time, simpleDateFormat)
                itemView.tvAmount.text = convertAmountToDisplayAmountStr(it.amount)
                itemView.tvState.setText(
                    when (it.state) {
                        0 -> R.string.bank_borrowing_state_borrowed
                        else -> R.string.bank_borrowing_state_borrowing_failed
                    }
                )
                itemView.tvState.setTextColor(
                    getColorByAttrId(
                        android.R.attr.textColor,
                        itemView.context
                    )
                )
            }
        }
    }
}