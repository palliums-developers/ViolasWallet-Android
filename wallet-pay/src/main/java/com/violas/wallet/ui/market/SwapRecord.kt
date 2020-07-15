package com.violas.wallet.ui.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.formatDate
import com.palliums.utils.getColorByAttrId
import com.palliums.violas.http.MarketSwapRecordDTO
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ExchangeManager
import com.violas.wallet.utils.convertViolasTokenUnit
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.item_market_swap_record.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
class SwapRecordActivity : BasePagingActivity<MarketSwapRecordDTO>() {

    private val viewModel by lazy {
        ViewModelProvider(this).get(SwapRecordViewModel::class.java)
    }
    private val viewAdapter by lazy {
        SwapRecordViewAdapter(
            retryCallback = { viewModel.retry() },
            clickItemCallback = {
                SwapDetailsActivity.start(this, it)
            },
            clickRetryCallback = { swapRecord, position ->
                // TODO 重试
            }
        )
    }

    override fun getViewModel(): PagingViewModel<MarketSwapRecordDTO> {
        return viewModel
    }

    override fun getViewAdapter(): PagingViewAdapter<MarketSwapRecordDTO> {
        return viewAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.swap_records)
        WalletAppViewModel.getViewModelInstance().mExistsAccountLiveData
            .observe(this, Observer {
                if (!it) {
                    initNotLoginView()
                    return@Observer

                }

                launch {
                    val initResult = viewModel.initAddress()
                    if (!initResult) {
                        initNotLoginView()
                        return@launch
                    }

                    mPagingHandler.start()
                }
            })
    }

    private fun initNotLoginView() {
        getRefreshLayout()?.setEnableRefresh(false)
        getRefreshLayout()?.setEnableLoadMore(false)
        getStatusLayout()?.showStatus(IStatusLayout.Status.STATUS_EMPTY)
    }
}

class SwapRecordViewModel : PagingViewModel<MarketSwapRecordDTO>() {

    private val exchangeManager by lazy { ExchangeManager() }

    private lateinit var address: String

    suspend fun initAddress() = withContext(Dispatchers.IO) {
        val violasAccount =
            AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
                ?: return@withContext false

        address = violasAccount.address
        return@withContext true
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<MarketSwapRecordDTO>, Any?) -> Unit
    ) {
        val swapRecords =
            exchangeManager.mViolasService.getMarketSwapRecords(
                address, pageSize, (pageNumber - 1) * pageSize
            )
        onSuccess.invoke(swapRecords ?: emptyList(), null)
        //onSuccess.invoke(mockData(), null)
    }

    private suspend fun mockData(): List<MarketSwapRecordDTO> {
        delay(2000)
        return mutableListOf(
            MarketSwapRecordDTO(
                fromName = "VLSUSD",
                fromAmount = "10000000",
                toName = "VLSSGD",
                toAmount = "13909000",
                version = 1,
                date = System.currentTimeMillis(),
                status = 4001
            ),
            MarketSwapRecordDTO(
                fromName = "VLSUSD",
                fromAmount = "100000",
                toName = "VLSEUR",
                toAmount = "87770",
                version = 2,
                date = System.currentTimeMillis(),
                status = 4002
            )
        )
    }
}

class SwapRecordViewAdapter(
    retryCallback: () -> Unit,
    private val clickItemCallback: ((MarketSwapRecordDTO) -> Unit)? = null,
    private val clickRetryCallback: ((MarketSwapRecordDTO, Int) -> Unit)? = null
) : PagingViewAdapter<MarketSwapRecordDTO>(retryCallback, SwapRecordDiffCallback()) {

    private val simpleDateFormat = SimpleDateFormat("MM.dd HH:mm:ss", Locale.ENGLISH)

    override fun onCreateViewHolderSupport(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<out Any> {
        return SwapRecordViewHolder(
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

class SwapRecordViewHolder(
    view: View,
    private val simpleDateFormat: SimpleDateFormat,
    private val clickItemCallback: ((MarketSwapRecordDTO) -> Unit)? = null,
    private val clickRetryCallback: ((MarketSwapRecordDTO, Int) -> Unit)? = null
) : BaseViewHolder<MarketSwapRecordDTO>(view) {

    init {
        itemView.setOnClickListener(this)
        itemView.tvRetry.setOnClickListener(this)
    }

    override fun onViewBind(itemPosition: Int, itemData: MarketSwapRecordDTO?) {
        itemData?.let {
            itemView.tvTime.text = formatDate(it.date, simpleDateFormat)
            itemView.tvFromToken.text = "${convertViolasTokenUnit(it.fromAmount)} ${it.fromName}"
            itemView.tvToToken.text = "${convertViolasTokenUnit(it.toAmount)} ${it.toName}"
            if (it.status == 4001) {
                itemView.tvRetry.visibility = View.GONE
                itemView.tvState.setText(R.string.market_swap_state_succeeded)
                itemView.tvState.setTextColor(
                    getColorByAttrId(R.attr.textColorSuccess, itemView.context)
                )
            } else {
                itemView.tvRetry.visibility = View.VISIBLE
                itemView.tvState.setText(R.string.market_swap_state_failed)
                itemView.tvState.setTextColor(
                    getColorByAttrId(R.attr.textColorFailure, itemView.context)
                )
            }
        }
    }

    override fun onViewClick(view: View, itemPosition: Int, itemData: MarketSwapRecordDTO?) {
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

class SwapRecordDiffCallback : DiffUtil.ItemCallback<MarketSwapRecordDTO>() {
    override fun areItemsTheSame(
        oldItem: MarketSwapRecordDTO,
        newItem: MarketSwapRecordDTO
    ): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }

    override fun areContentsTheSame(
        oldItem: MarketSwapRecordDTO,
        newItem: MarketSwapRecordDTO
    ): Boolean {
        return oldItem == newItem
    }
}