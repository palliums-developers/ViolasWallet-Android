package com.violas.wallet.ui.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import com.palliums.base.BaseViewHolder
import com.palliums.extensions.expandTouchArea
import com.palliums.extensions.lazyLogError
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.exceptionAsync
import com.palliums.utils.formatDate
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.getString
import com.palliums.violas.http.MarketSwapRecordDTO
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ExchangeManager
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.item_market_swap_record.view.*
import kotlinx.coroutines.*
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

        setTitle(R.string.title_market_swap_records)
        mPagingHandler.init()
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

                    mPagingHandler.start(5, true)
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
    private val crossChainExchangeService by lazy {
        DataRepository.getMappingExchangeService()
    }

    private lateinit var violasAddress: String
    private lateinit var libraAddress: String
    private lateinit var bitcoinAddress: String

    private var vlsHasMoreData = true
    private var vlsCrossChainHasMoreData = true
    private var lbrCrossChainHasMoreData = true
    private var btcCrossChainHasMoreData = true

    suspend fun initAddress() = withContext(Dispatchers.IO) {
        val violasAccount =
            AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
                ?: return@withContext false
        val libraAccount =
            AccountManager().getIdentityByCoinType(CoinTypes.Libra.coinType())
                ?: return@withContext false
        val bitcoinAccount =
            AccountManager().getIdentityByCoinType(
                if (Vm.TestNet) CoinTypes.BitcoinTest.coinType() else CoinTypes.Bitcoin.coinType()
            ) ?: return@withContext false

        violasAddress = violasAccount.address
        libraAddress = libraAccount.address
        bitcoinAddress = bitcoinAccount.address
        return@withContext true
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<MarketSwapRecordDTO>, Any?) -> Unit
    ) {
        if (pageNumber == 1) {
            // 刷新动作，重制标记
            vlsHasMoreData = true
            vlsCrossChainHasMoreData = true
            lbrCrossChainHasMoreData = true
            btcCrossChainHasMoreData = true
        }
        lazyLogError("SwapRecord") {
            "loadData. vlsHasMoreData($vlsHasMoreData), vlsCrossChainHasMoreData($vlsCrossChainHasMoreData), " +
                    "lbrCrossChainHasMoreData($lbrCrossChainHasMoreData), btcCrossChainHasMoreData($btcCrossChainHasMoreData)"
        }

        val offset = (pageNumber - 1) * pageSize
        val swapRecords = coroutineScope {

            val vlsSwapRecordsDeferred =
                if (vlsHasMoreData)
                    exceptionAsync {
                        exchangeManager.mViolasService.getMarketSwapRecords(
                            violasAddress, pageSize, offset
                        )
                    }
                else
                    null

            val vlsCrossChainSwapRecordsDeferred =
                if (vlsCrossChainHasMoreData)
                    exceptionAsync {
                        crossChainExchangeService.getCrossChainSwapRecords(
                            violasAddress, "violas", pageSize, offset
                        )
                    }
                else
                    null

            val lbrCrossChainSwapRecordsDeferred =
                if (lbrCrossChainHasMoreData)
                    exceptionAsync {
                        crossChainExchangeService.getCrossChainSwapRecords(
                            libraAddress, "libra", pageSize, offset
                        )
                    }
                else
                    null

            val btcCrossChainSwapRecordsDeferred =
                if (btcCrossChainHasMoreData)
                    exceptionAsync {
                        crossChainExchangeService.getCrossChainSwapRecords(
                            bitcoinAddress, "btc", pageSize, offset
                        )
                    }
                else
                    null

            val vlsSwapRecords =
                vlsSwapRecordsDeferred?.await()
            val vlsCrossChainSwapRecords =
                vlsCrossChainSwapRecordsDeferred?.await()
            val lbrCrossChainSwapRecords =
                lbrCrossChainSwapRecordsDeferred?.await()
            val btcCrossChainSwapRecords =
                btcCrossChainSwapRecordsDeferred?.await()

            val records = mutableListOf<MarketSwapRecordDTO>()
            vlsSwapRecords?.forEach {
                records.add(it.apply {
                    customStatus = if (status == 4001)
                        MarketSwapRecordDTO.Status.SUCCEEDED
                    else
                        MarketSwapRecordDTO.Status.FAILED
                })
            }
            vlsHasMoreData = if (vlsSwapRecords == null)
                false
            else
                vlsSwapRecords.size >= pageSize

            vlsCrossChainSwapRecords?.forEach {
                records.add(
                    MarketSwapRecordDTO(
                        fromName = it.coinA,
                        fromAmount = it.amountA,
                        toName = it.coinB,
                        toAmount = it.amountB,
                        gasUsed = null,
                        gasCurrency = null,
                        version = it.version,
                        date = it.date,
                        status = it.status,
                        customStatus = when (it.status) {
                            4001 -> MarketSwapRecordDTO.Status.SUCCEEDED
                            4002 -> MarketSwapRecordDTO.Status.PROCESSING
                            4004 -> MarketSwapRecordDTO.Status.CANCELLED
                            else -> MarketSwapRecordDTO.Status.FAILED
                        }
                    )
                )
            }
            vlsCrossChainHasMoreData = if (vlsCrossChainSwapRecords == null)
                false
            else
                vlsCrossChainSwapRecords.size >= pageSize

            lbrCrossChainSwapRecords?.forEach {
                records.add(
                    MarketSwapRecordDTO(
                        fromName = it.coinA,
                        fromAmount = it.amountA,
                        toName = it.coinB,
                        toAmount = it.amountB,
                        gasUsed = null,
                        gasCurrency = null,
                        version = it.version,
                        date = it.date,
                        status = it.status,
                        customStatus = when (it.status) {
                            4001 -> MarketSwapRecordDTO.Status.SUCCEEDED
                            4002 -> MarketSwapRecordDTO.Status.PROCESSING
                            4004 -> MarketSwapRecordDTO.Status.CANCELLED
                            else -> MarketSwapRecordDTO.Status.FAILED
                        }
                    )
                )
            }
            lbrCrossChainHasMoreData = if (lbrCrossChainSwapRecords == null)
                false
            else
                lbrCrossChainSwapRecords.size >= pageSize

            btcCrossChainSwapRecords?.forEach {
                records.add(
                    MarketSwapRecordDTO(
                        fromName = it.coinA,
                        fromAmount = it.amountA,
                        toName = it.coinB,
                        toAmount = it.amountB,
                        gasUsed = null,
                        gasCurrency = null,
                        version = it.version,
                        date = it.date,
                        status = it.status,
                        customStatus = when (it.status) {
                            4001 -> MarketSwapRecordDTO.Status.SUCCEEDED
                            4002 -> MarketSwapRecordDTO.Status.PROCESSING
                            4004 -> MarketSwapRecordDTO.Status.CANCELLED
                            else -> MarketSwapRecordDTO.Status.FAILED
                        }
                    )
                )
            }
            btcCrossChainHasMoreData = if (lbrCrossChainSwapRecords == null)
                false
            else
                lbrCrossChainSwapRecords.size >= pageSize

            records
        }

        onSuccess.invoke(swapRecords, null)
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
                gasUsed = "",
                gasCurrency = "",
                version = 1,
                date = System.currentTimeMillis(),
                status = 4001,
                customStatus = MarketSwapRecordDTO.Status.SUCCEEDED
            ),
            MarketSwapRecordDTO(
                fromName = "VLSUSD",
                fromAmount = "100000",
                toName = "VLSEUR",
                toAmount = "87770",
                gasUsed = "",
                gasCurrency = "",
                version = 2,
                date = System.currentTimeMillis(),
                status = 4002,
                customStatus = MarketSwapRecordDTO.Status.FAILED
            ),
            MarketSwapRecordDTO(
                fromName = "VLSUSD",
                fromAmount = "10000000",
                toName = "VLSGBP",
                toAmount = "211111",
                gasUsed = "",
                gasCurrency = "",
                version = 1,
                date = System.currentTimeMillis(),
                status = 4003,
                customStatus = MarketSwapRecordDTO.Status.PROCESSING
            ),
            MarketSwapRecordDTO(
                fromName = "VLSUSD",
                fromAmount = "100000",
                toName = "BTC",
                toAmount = "1000",
                gasUsed = "",
                gasCurrency = "",
                version = 2,
                date = System.currentTimeMillis(),
                status = 4004,
                customStatus = MarketSwapRecordDTO.Status.CANCELLED
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

            itemView.tvFromToken.text =
                if (it.fromName.isNullOrBlank() || it.fromAmount.isNullOrBlank()) {
                    getString(R.string.value_null)
                } else {
                    "${convertAmountToDisplayAmountStr(it.fromAmount!!)} ${it.fromName}"
                }

            itemView.tvToToken.text =
                if (it.toName.isNullOrBlank() || it.toAmount.isNullOrBlank()) {
                    getString(R.string.value_null)
                } else {
                    "${convertAmountToDisplayAmountStr(it.toAmount!!)} ${it.toName}"
                }

            when (it.customStatus) {
                MarketSwapRecordDTO.Status.SUCCEEDED -> {
                    itemView.tvRetry.visibility = View.GONE
                    itemView.tvState.setText(R.string.market_swap_state_succeeded)
                    itemView.tvState.setTextColor(
                        getColorByAttrId(R.attr.textColorSuccess, itemView.context)
                    )
                }

                MarketSwapRecordDTO.Status.PROCESSING -> {
                    // TODO 取消先隐藏
                    itemView.tvRetry.visibility = View.GONE
                    itemView.tvRetry.expandTouchArea()
                    itemView.tvState.setText(R.string.market_swap_state_processing)
                    itemView.tvState.setTextColor(
                        getColorByAttrId(R.attr.textColorProcessing, itemView.context)
                    )
                }

                MarketSwapRecordDTO.Status.CANCELLED -> {
                    itemView.tvRetry.visibility = View.GONE
                    itemView.tvState.setText(R.string.market_swap_state_cancelled)
                    itemView.tvState.setTextColor(
                        getColorByAttrId(android.R.attr.textColorTertiary, itemView.context)
                    )
                }

                else -> {
                    itemView.tvRetry.visibility = View.GONE
                    itemView.tvState.setText(R.string.market_swap_state_failed)
                    itemView.tvState.setTextColor(
                        getColorByAttrId(R.attr.textColorFailure, itemView.context)
                    )
                }
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