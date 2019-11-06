package com.violas.wallet.ui.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseViewHolder
import com.violas.wallet.base.paging.BasePagingActivity
import com.violas.wallet.base.paging.PagingViewAdapter
import com.violas.wallet.base.paging.PagingViewModel
import com.violas.wallet.base.recycler.DividerItemDecoration
import com.violas.wallet.getColor
import kotlinx.android.synthetic.main.item_transaction_record.view.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * Created by elephant on 2019-11-06 17:15.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易记录页面
 */
class TransactionRecordActivity : BasePagingActivity<TransactionRecordVO>() {

    override fun initViewModel(): PagingViewModel<TransactionRecordVO> {
        return TransactionRecordViewModel()
    }

    override fun initViewAdapter(): PagingViewAdapter<TransactionRecordVO> {
        return TransactionRecordViewAdapter { getViewModel().retry() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.transaction_record_title)

        getRecyclerView().addItemDecoration(
            DividerItemDecoration(this, LinearLayout.VERTICAL).apply {
                setDrawable(getDrawable(R.drawable.shape_transaction_record_divider)!!)
            }
        )

        mPagingHandler.start()
    }
}

data class TransactionRecordVO(
    val id: Int,
    val coinTypes: CoinTypes,
    val transactionType: Int,
    val time: Long,
    val amount: Double,
    val address: String
)

class TransactionRecordViewHolder(view: View, private val mSimpleDateFormat: SimpleDateFormat) :
    BaseViewHolder<TransactionRecordVO>(view) {

    init {
        itemView.vQuery.setOnClickListener(this)
    }

    override fun onViewBind(itemIndex: Int, itemDate: TransactionRecordVO?) {
        itemDate?.let {
            itemView.vTime.text = mSimpleDateFormat.format(it.time)
            itemView.vAmount.text = "${it.amount} ${it.coinTypes.coinUnit()}"
            itemView.vCoinName.text = it.coinTypes.coinName()
            itemView.vAddress.text = it.address
            when (it.transactionType) {
                1 -> {
                    itemView.vType.setText(R.string.transaction_record_receipt)
                    itemView.vType.setTextColor(getColor(R.color.color_13B788))
                }

                else -> {
                    itemView.vType.setText(R.string.transaction_record_transfer)
                    itemView.vType.setTextColor(getColor(R.color.color_E54040))
                }
            }
        }
    }

    override fun onViewClick(view: View, itemIndex: Int, itemDate: TransactionRecordVO?) {
        itemDate?.let {
            // TODO 浏览器查询
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

class TransactionRecordViewAdapter(retryCallback: () -> Unit) :
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
            ), mSimpleDateFormat
        )
    }
}

class TransactionRecordViewModel : PagingViewModel<TransactionRecordVO>() {

    override suspend fun loadData(
        pageSize: Int,
        offset: Int,
        onSuccess: (List<TransactionRecordVO>, Int) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        // TODO 对接接口
        onSuccess.invoke(fakeData(pageSize, offset), pageSize)
    }

    /**
     * code for test
     */
    private suspend fun fakeData(pageSize: Int, offset: Int): List<TransactionRecordVO> {
        delay(3000)

        val list = mutableListOf<TransactionRecordVO>()
        repeat(pageSize) {
            val vo = TransactionRecordVO(
                id = offset + it + 1,
                coinTypes = when (it % 3) {
                    0 -> CoinTypes.Bitcoin
                    1 -> CoinTypes.Libra
                    else -> CoinTypes.VToken
                },
                transactionType = it % 2,
                time = System.currentTimeMillis(),
                amount = Random.nextDouble(),
                address = "mkYUsJ8N1AidNUySQGCpwswQUaoyL2Mu8L00000000000000${offset + it + 1}"
            )

            list.add(vo)
        }

        return list
    }
}