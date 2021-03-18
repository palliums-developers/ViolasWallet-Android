package com.violas.wallet.ui.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.extensions.expandTouchArea
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.formatDate
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.getString
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.exchange.ViolasSwapRecordDTO
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.item_market_swap_record.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2020/7/14 15:22.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易市场兑换记录页面
 */
class SwapRecordActivity : BasePagingActivity<ViolasSwapRecordDTO>() {

    override fun lazyInitPagingViewModel(): PagingViewModel<ViolasSwapRecordDTO> {
        return ViewModelProvider(this).get(SwapRecordViewModel::class.java)
    }

    override fun lazyInitPagingViewAdapter(): PagingViewAdapter<ViolasSwapRecordDTO> {
        return ViewAdapter(
            clickItemCallback = {
                SwapDetailsActivity.start(this, it)
            },
            clickRetryCallback = { swapRecord, position ->
                // TODO 重试
            }
        )
    }

    fun getViewModel(): SwapRecordViewModel {
        return getPagingViewModel() as SwapRecordViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.swap_records_title)
        getPagingHandler().init()
        WalletAppViewModel.getInstance().mExistsAccountLiveData
            .observe(this, Observer {
                if (!it) {
                    initNotLoginView()
                    return@Observer
                }

                launch {
                    val initResult = getViewModel().initAddress()
                    if (!initResult) {
                        initNotLoginView()
                        return@launch
                    }

                    getPagingHandler().start()
                }
            })
    }

    private fun initNotLoginView() {
        getRefreshLayout()?.setEnableRefresh(false)
        getRefreshLayout()?.setEnableLoadMore(false)
        getStatusLayout()?.showStatus(IStatusLayout.Status.STATUS_EMPTY)
    }

    class ViewAdapter(
        private val clickItemCallback: ((ViolasSwapRecordDTO) -> Unit)? = null,
        private val clickRetryCallback: ((ViolasSwapRecordDTO, Int) -> Unit)? = null
    ) : PagingViewAdapter<ViolasSwapRecordDTO>() {

        private val simpleDateFormat = SimpleDateFormat("MM.dd HH:mm:ss", Locale.ENGLISH)

        override fun onCreateViewHolderSupport(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<out Any> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_market_swap_record,
                    parent,
                    false
                ),
                simpleDateFormat,
                clickItemCallback,
                clickRetryCallback
            )
        }
    }

    class ViewHolder(
        view: View,
        private val simpleDateFormat: SimpleDateFormat,
        private val clickItemCallback: ((ViolasSwapRecordDTO) -> Unit)? = null,
        private val clickRetryCallback: ((ViolasSwapRecordDTO, Int) -> Unit)? = null
    ) : BaseViewHolder<ViolasSwapRecordDTO>(view) {

        init {
            itemView.setOnClickListener(this)
            itemView.tvRetry.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: ViolasSwapRecordDTO?) {
            itemData?.let {
                itemView.tvTime.text = formatDate(it.time, simpleDateFormat)

                itemView.tvInputCoin.text =
                    if (it.inputDisplayName.isNullOrBlank() || it.inputCoinAmount.isNullOrBlank()) {
                        getString(R.string.common_desc_value_null)
                    } else {
                        "${convertAmountToDisplayAmountStr(it.inputCoinAmount)} ${it.inputDisplayName}"
                    }

                itemView.tvOutputCoin.text =
                    if (it.outputDisplayName.isNullOrBlank() || it.outputCoinAmount.isNullOrBlank()) {
                        getString(R.string.common_desc_value_null)
                    } else {
                        "${convertAmountToDisplayAmountStr(it.outputCoinAmount)} ${it.outputDisplayName}"
                    }

                when {
                    it.status.isNullOrBlank() -> {
                        // TODO 取消先隐藏
                        itemView.tvRetry.visibility = View.GONE
                        itemView.tvRetry.expandTouchArea()
                        itemView.tvState.setText(R.string.swap_txn_state_processing)
                        itemView.tvState.setTextColor(
                            getColorByAttrId(R.attr.textColorProcessing, itemView.context)
                        )
                    }


                    it.status.equals("Executed", true) -> {
                        itemView.tvRetry.visibility = View.GONE
                        itemView.tvState.setText(R.string.swap_txn_state_succeeded)
                        itemView.tvState.setTextColor(
                            getColorByAttrId(R.attr.textColorSuccess, itemView.context)
                        )
                    }

                    it.status.equals("Cancel", true)
                            || it.status.equals("Canceled", true)
                            || it.status.equals("Cancelled", true) -> {
                        itemView.tvRetry.visibility = View.GONE
                        itemView.tvState.setText(R.string.common_state_cancelled)
                        itemView.tvState.setTextColor(
                            getColorByAttrId(android.R.attr.textColorTertiary, itemView.context)
                        )
                    }

                    else -> {
                        itemView.tvRetry.visibility = View.GONE
                        itemView.tvState.setText(R.string.swap_txn_state_failed)
                        itemView.tvState.setTextColor(
                            getColorByAttrId(R.attr.textColorFailure, itemView.context)
                        )
                    }
                }
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: ViolasSwapRecordDTO?) {
            itemData?.let {
                when (view) {
                    itemView -> clickItemCallback?.invoke(it)
                    itemView.tvRetry -> clickRetryCallback?.invoke(it, itemPosition)
                    else -> {
                        // ignore
                    }
                }
            }
        }
    }
}

class SwapRecordViewModel : PagingViewModel<ViolasSwapRecordDTO>() {

    private val exchangeService by lazy {
        DataRepository.getExchangeService()
    }

    private lateinit var violasWalletAddress: String

    suspend fun initAddress() = withContext(Dispatchers.IO) {
        val violasAccount =
            AccountManager.getAccountByCoinNumber(getViolasCoinType().coinNumber())
                ?: return@withContext false

        violasWalletAddress = violasAccount.address
        return@withContext true
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<ViolasSwapRecordDTO>, Any?) -> Unit
    ) {
        val list = exchangeService.getViolasSwapRecords(
            violasWalletAddress,
            pageSize,
            (pageNumber - 1) * pageSize
        )
        onSuccess.invoke(list, null)
    }
}