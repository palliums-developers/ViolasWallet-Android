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
import com.violas.wallet.common.KEY_TWO
import com.violas.wallet.repository.http.bank.RepaymentDetailDTO
import com.violas.wallet.ui.bank.details.BaseBankDetailFragment
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import kotlinx.android.synthetic.main.fragment_bank_repayment_detail.*
import kotlinx.android.synthetic.main.item_bank_repayment_detail.view.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2020/8/28 15:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 还款明细视图
 */
class RepaymentDetailFragment : BaseBankDetailFragment<RepaymentDetailDTO>() {

    companion object {
        fun newInstance(productId: String, walletAddress: String): RepaymentDetailFragment {
            return RepaymentDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_ONE, productId)
                    putString(KEY_TWO, walletAddress)
                }
            }
        }
    }

    private val viewModel by lazy {
        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    return modelClass
                        .getConstructor(String::class.java, String::class.java)
                        .newInstance(mProductId, mWalletAddress)
                }
            }
        ).get(RepaymentDetailViewModel::class.java)
    }
    private val viewAdapter by lazy { ViewAdapter() }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_bank_repayment_detail
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

    override fun getViewModel(): PagingViewModel<RepaymentDetailDTO> {
        return viewModel
    }

    override fun getViewAdapter(): PagingViewAdapter<RepaymentDetailDTO> {
        return viewAdapter
    }

    class ViewAdapter : PagingViewAdapter<RepaymentDetailDTO>() {

        private val simpleDateFormat by lazy {
            SimpleDateFormat("HH:mm:ss MM/dd", Locale.ENGLISH)
        }

        override fun onCreateViewHolderSupport(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<out Any> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_bank_repayment_detail,
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
    ) : BaseViewHolder<RepaymentDetailDTO>(view) {

        override fun onViewBind(itemPosition: Int, itemData: RepaymentDetailDTO?) {
            itemData?.let {
                itemView.tvTime.text = formatDate(it.time, simpleDateFormat)
                itemView.tvAmount.text = convertAmountToDisplayAmountStr(it.amount)
                itemView.tvState.setText(
                    when (it.state) {
                        0 -> R.string.bank_borrowing_state_repaying
                        1 -> R.string.bank_borrowing_state_repaid
                        else -> R.string.bank_borrowing_state_repayment_failed
                    }
                )
                itemView.tvState.setTextColor(
                    getColorByAttrId(
                        when (it.state) {
                            0 -> R.attr.textColorProcessing
                            else -> android.R.attr.textColor
                        },
                        itemView.context
                    )
                )
            }
        }
    }
}

class RepaymentDetailViewModel(
    private val productId: String,
    private val walletAddress: String
) : PagingViewModel<RepaymentDetailDTO>() {

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<RepaymentDetailDTO>, Any?) -> Unit
    ) {
        // TODO 对接接口
        delay(2000)
        onSuccess.invoke(fakeData(), null)
    }

    private fun fakeData(): List<RepaymentDetailDTO> {
        return mutableListOf(
            RepaymentDetailDTO("111010110", System.currentTimeMillis(), 0),
            RepaymentDetailDTO("222020220", System.currentTimeMillis(), 1),
            RepaymentDetailDTO("333030333", System.currentTimeMillis(), 2)
        )
    }
}