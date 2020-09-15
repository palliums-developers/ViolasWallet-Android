package com.violas.wallet.ui.mapping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.formatDate
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.violas.wallet.repository.http.mapping.MappingRecordDTO
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.str2CoinType
import kotlinx.android.synthetic.main.item_mapping_record.view.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2020/8/14 17:45.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MappingRecordActivity : BasePagingActivity<MappingRecordDTO>() {

    private val viewModel by lazy {
        ViewModelProvider(this).get(MappingRecordViewModel::class.java)
    }
    private val viewAdapter by lazy {
        MappingRecordViewAdapter {
            // TODO 点击处理
        }
    }

    override fun getViewModel(): PagingViewModel<MappingRecordDTO> {
        return viewModel
    }

    override fun getViewAdapter(): PagingViewAdapter<MappingRecordDTO> {
        return viewAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.mapping_record)
        mPagingHandler.init()

        launch {
            val initResult = viewModel.initAddress()
            if (initResult) {
                mPagingHandler.start()
            } else {
                close()
            }
        }
    }

    class MappingRecordViewAdapter(
        private val clickItemCallback: ((MappingRecordDTO) -> Unit)? = null
    ) : PagingViewAdapter<MappingRecordDTO>() {

        private val simpleDateFormat = SimpleDateFormat("MM.dd HH:mm:ss", Locale.ENGLISH)

        override fun onCreateViewHolderSupport(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<out Any> {
            return MappingRecordViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_mapping_record,
                    parent,
                    false
                ),
                simpleDateFormat,
                clickItemCallback
            )
        }
    }

    class MappingRecordViewHolder(
        view: View,
        private val simpleDateFormat: SimpleDateFormat,
        private val clickItemCallback: ((MappingRecordDTO) -> Unit)? = null
    ) : BaseViewHolder<MappingRecordDTO>(view) {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: MappingRecordDTO?) {
            itemData?.let {
                itemView.tvTime.text = formatDate(it.time, simpleDateFormat)

                itemView.tvInputCoin.text =
                    if (it.inputCoinDisplayName.isNullOrBlank()
                        || it.inputCoinAmount.isNullOrBlank()
                    ) {
                        getString(R.string.value_null)
                    } else {
                        "${convertAmountToDisplayAmountStr(
                            it.inputCoinAmount,
                            str2CoinType(it.inputChainName)
                        )} ${it.inputCoinDisplayName}"
                    }

                itemView.tvOutputCoin.text =
                    if (it.outputCoinDisplayName.isNullOrBlank()
                        || it.outputCoinAmount.isNullOrBlank()
                    ) {
                        getString(R.string.value_null)
                    } else {
                        "${convertAmountToDisplayAmountStr(
                            it.outputCoinAmount,
                            str2CoinType(it.outputChainName)
                        )} ${it.outputCoinDisplayName}"
                    }

                itemView.tvMinerFees.text =
                    getString(R.string.gas_fee_format, getString(R.string.value_null))

                when {
                    it.state?.equals("end", true) == true -> {
                        itemView.tvState.setText(R.string.mapping_state_succeeded)
                        itemView.tvState.setTextColor(
                            getColorByAttrId(R.attr.textColorSuccess, itemView.context)
                        )
                    }

                    it.state?.equals("start", true) == true -> {
                        itemView.tvState.setText(R.string.mapping_state_processing)
                        itemView.tvState.setTextColor(
                            getColorByAttrId(R.attr.textColorProcessing, itemView.context)
                        )
                    }

                    it.state?.equals("cancel", true) == true -> {
                        itemView.tvState.setText(R.string.mapping_state_cancelled)
                        itemView.tvState.setTextColor(
                            getColorByAttrId(android.R.attr.textColorTertiary, itemView.context)
                        )
                    }

                    else -> {
                        itemView.tvState.setText(R.string.mapping_state_failed)
                        itemView.tvState.setTextColor(
                            getColorByAttrId(R.attr.textColorFailure, itemView.context)
                        )
                    }
                }
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: MappingRecordDTO?) {
            itemData?.let {
                when (view) {
                    itemView -> clickItemCallback?.invoke(it)
                    else -> {
                        // ignore
                    }
                }
            }
        }
    }
}