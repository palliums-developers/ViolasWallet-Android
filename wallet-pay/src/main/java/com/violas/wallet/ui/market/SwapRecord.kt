package com.violas.wallet.ui.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.extensions.expandTouchArea
import com.palliums.extensions.logInfo
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.*
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.exchange.SwapRecordDTO
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.str2CoinType
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
class SwapRecordActivity : BasePagingActivity<SwapRecordDTO>() {

    private val viewModel by lazy {
        ViewModelProvider(this).get(SwapRecordViewModel::class.java)
    }
    private val viewAdapter by lazy {
        SwapRecordViewAdapter(
            clickItemCallback = {
                SwapDetailsActivity.start(this, it)
            },
            clickRetryCallback = { swapRecord, position ->
                // TODO 重试
            }
        )
    }

    override fun getViewModel(): PagingViewModel<SwapRecordDTO> {
        return viewModel
    }

    override fun getViewAdapter(): PagingViewAdapter<SwapRecordDTO> {
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

class SwapRecordViewModel : PagingViewModel<SwapRecordDTO>() {

    private val exchangeService by lazy {
        DataRepository.getExchangeService()
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
        onSuccess: (List<SwapRecordDTO>, Any?) -> Unit
    ) {
        if (pageNumber == 1) {
            // 刷新动作，重制标记
            vlsHasMoreData = true
            vlsCrossChainHasMoreData = true
            lbrCrossChainHasMoreData = true
            btcCrossChainHasMoreData = true
        }
        logInfo("SwapRecord") {
            "loadData. vlsHasMoreData($vlsHasMoreData), vlsCrossChainHasMoreData($vlsCrossChainHasMoreData), " +
                    "lbrCrossChainHasMoreData($lbrCrossChainHasMoreData), btcCrossChainHasMoreData($btcCrossChainHasMoreData)"
        }

        val offset = (pageNumber - 1) * pageSize
        val swapRecords = coroutineScope {

            val vlsSwapRecordsDeferred =
                if (vlsHasMoreData)
                    exceptionAsync {
                        exchangeService.getSwapRecords(
                            violasAddress, pageSize, offset
                        )
                    }
                else
                    null

            val vlsCrossChainSwapRecordsDeferred =
                if (vlsCrossChainHasMoreData)
                    exceptionAsync {
                        exchangeService.getCrossChainSwapRecords(
                            violasAddress, "violas", pageSize, offset
                        )
                    }
                else
                    null

            val lbrCrossChainSwapRecordsDeferred =
                if (lbrCrossChainHasMoreData)
                    exceptionAsync {
                        exchangeService.getCrossChainSwapRecords(
                            libraAddress, "libra", pageSize, offset
                        )
                    }
                else
                    null

            val btcCrossChainSwapRecordsDeferred =
                if (btcCrossChainHasMoreData)
                    exceptionAsync {
                        exchangeService.getCrossChainSwapRecords(
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

            val records = mutableListOf<SwapRecordDTO>()
            vlsSwapRecords?.forEach {
                records.add(it.apply {
                    inputCoinType = CoinTypes.Violas.coinType()
                    outputCoinType = CoinTypes.Violas.coinType()
                    customStatus = if (status == 4001)
                        SwapRecordDTO.Status.SUCCEEDED
                    else
                        SwapRecordDTO.Status.FAILED
                })
            }
            vlsHasMoreData = if (vlsSwapRecords == null)
                false
            else
                vlsSwapRecords.size >= pageSize

            vlsCrossChainSwapRecords?.forEach {
                records.add(
                    SwapRecordDTO(
                        inputCoinName = it.inputCoinDisplayName ?: it.inputCoinName,
                        inputCoinAmount = it.inputCoinAmount,
                        outputCoinName = it.outputCoinDisplayName ?: it.outputCoinName,
                        outputCoinAmount = it.outputCoinAmount,
                        gasCoinName = null,
                        gasCoinAmount = null,
                        time = it.time,
                        confirmedTime = it.confirmedTime,
                        version = it.version,
                        status = it.status,
                        inputCoinType = str2CoinType(it.inputChainName).coinType(),
                        outputCoinType = str2CoinType(it.outputChainName).coinType(),
                        customStatus = when (it.status) {
                            4001 -> SwapRecordDTO.Status.SUCCEEDED
                            4002 -> SwapRecordDTO.Status.PROCESSING
                            4004 -> SwapRecordDTO.Status.CANCELLED
                            else -> SwapRecordDTO.Status.FAILED
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
                    SwapRecordDTO(
                        inputCoinName = it.inputCoinDisplayName ?: it.inputCoinName,
                        inputCoinAmount = it.inputCoinAmount,
                        outputCoinName = it.outputCoinDisplayName ?: it.outputCoinName,
                        outputCoinAmount = it.outputCoinAmount,
                        gasCoinAmount = null,
                        gasCoinName = null,
                        time = it.time,
                        confirmedTime = it.confirmedTime,
                        version = it.version,
                        status = it.status,
                        inputCoinType = str2CoinType(it.inputChainName).coinType(),
                        outputCoinType = str2CoinType(it.outputChainName).coinType(),
                        customStatus = when (it.status) {
                            4001 -> SwapRecordDTO.Status.SUCCEEDED
                            4002 -> SwapRecordDTO.Status.PROCESSING
                            4004 -> SwapRecordDTO.Status.CANCELLED
                            else -> SwapRecordDTO.Status.FAILED
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
                    SwapRecordDTO(
                        inputCoinName = it.inputCoinDisplayName ?: it.inputCoinName,
                        inputCoinAmount = it.inputCoinAmount,
                        outputCoinName = it.outputCoinDisplayName ?: it.outputCoinName,
                        outputCoinAmount = it.outputCoinAmount,
                        gasCoinAmount = null,
                        gasCoinName = null,
                        time = it.time,
                        confirmedTime = it.confirmedTime,
                        version = it.version,
                        status = it.status,
                        inputCoinType = str2CoinType(it.inputChainName).coinType(),
                        outputCoinType = str2CoinType(it.outputChainName).coinType(),
                        customStatus = when (it.status) {
                            4001 -> SwapRecordDTO.Status.SUCCEEDED
                            4002 -> SwapRecordDTO.Status.PROCESSING
                            4004 -> SwapRecordDTO.Status.CANCELLED
                            else -> SwapRecordDTO.Status.FAILED
                        }
                    )
                )
            }
            btcCrossChainHasMoreData = if (lbrCrossChainSwapRecords == null)
                false
            else
                lbrCrossChainSwapRecords.size >= pageSize

            records.sortByDescending { it.time }

            records
        }

        onSuccess.invoke(swapRecords, null)
        //onSuccess.invoke(mockData(), null)
    }

    private suspend fun mockData(): List<SwapRecordDTO> {
        delay(2000)
        return mutableListOf(
            SwapRecordDTO(
                inputCoinName = "VLSUSD",
                inputCoinAmount = "10000000",
                outputCoinName = "VLSSGD",
                outputCoinAmount = "13909000",
                gasCoinName = "",
                gasCoinAmount = "",
                time = System.currentTimeMillis(),
                confirmedTime = System.currentTimeMillis() + 1000,
                version = 1,
                status = 4001,
                customStatus = SwapRecordDTO.Status.SUCCEEDED
            ),
            SwapRecordDTO(
                inputCoinName = "VLSUSD",
                inputCoinAmount = "100000",
                outputCoinName = "VLSEUR",
                outputCoinAmount = "87770",
                gasCoinName = "",
                gasCoinAmount = "",
                time = System.currentTimeMillis(),
                confirmedTime = System.currentTimeMillis() + 1000,
                version = 2,
                status = 4002,
                customStatus = SwapRecordDTO.Status.FAILED
            ),
            SwapRecordDTO(
                inputCoinName = "VLSUSD",
                inputCoinAmount = "10000000",
                outputCoinName = "VLSGBP",
                outputCoinAmount = "211111",
                gasCoinName = "",
                gasCoinAmount = "",
                version = 1,
                time = System.currentTimeMillis(),
                confirmedTime = System.currentTimeMillis() + 1000,
                status = 4003,
                customStatus = SwapRecordDTO.Status.PROCESSING
            ),
            SwapRecordDTO(
                inputCoinName = "VLSUSD",
                inputCoinAmount = "100000",
                outputCoinName = "BTC",
                outputCoinAmount = "1000",
                gasCoinName = "",
                gasCoinAmount = "",
                time = System.currentTimeMillis(),
                confirmedTime = System.currentTimeMillis() + 1000,
                version = 2,
                status = 4004,
                customStatus = SwapRecordDTO.Status.CANCELLED
            )
        )
    }
}

class SwapRecordViewAdapter(
    private val clickItemCallback: ((SwapRecordDTO) -> Unit)? = null,
    private val clickRetryCallback: ((SwapRecordDTO, Int) -> Unit)? = null
) : PagingViewAdapter<SwapRecordDTO>() {

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
    private val clickItemCallback: ((SwapRecordDTO) -> Unit)? = null,
    private val clickRetryCallback: ((SwapRecordDTO, Int) -> Unit)? = null
) : BaseViewHolder<SwapRecordDTO>(view) {

    init {
        itemView.setOnClickListener(this)
        itemView.tvRetry.setOnClickListener(this)
    }

    override fun onViewBind(itemPosition: Int, itemData: SwapRecordDTO?) {
        itemData?.let {
            itemView.tvTime.text = if (it.inputCoinType == it.outputCoinType)
                formatDate(correctDateLength(it.confirmedTime) - 1000, simpleDateFormat)
            else
                formatDate(it.time, simpleDateFormat)

            itemView.tvInputCoin.text =
                if (it.inputCoinName.isNullOrBlank() || it.inputCoinAmount.isNullOrBlank()) {
                    getString(R.string.value_null)
                } else {
                    "${convertAmountToDisplayAmountStr(
                        it.inputCoinAmount,
                        CoinTypes.parseCoinType(it.inputCoinType)
                    )} ${it.inputCoinName}"
                }

            itemView.tvOutputCoin.text =
                if (it.outputCoinName.isNullOrBlank() || it.outputCoinAmount.isNullOrBlank()) {
                    getString(R.string.value_null)
                } else {
                    "${convertAmountToDisplayAmountStr(
                        it.outputCoinAmount,
                        CoinTypes.parseCoinType(it.outputCoinType)
                    )} ${it.outputCoinName}"
                }

            when (it.customStatus) {
                SwapRecordDTO.Status.SUCCEEDED -> {
                    itemView.tvRetry.visibility = View.GONE
                    itemView.tvState.setText(R.string.market_swap_state_succeeded)
                    itemView.tvState.setTextColor(
                        getColorByAttrId(R.attr.textColorSuccess, itemView.context)
                    )
                }

                SwapRecordDTO.Status.PROCESSING -> {
                    // TODO 取消先隐藏
                    itemView.tvRetry.visibility = View.GONE
                    itemView.tvRetry.expandTouchArea()
                    itemView.tvState.setText(R.string.market_swap_state_processing)
                    itemView.tvState.setTextColor(
                        getColorByAttrId(R.attr.textColorProcessing, itemView.context)
                    )
                }

                SwapRecordDTO.Status.CANCELLED -> {
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

    override fun onViewClick(view: View, itemPosition: Int, itemData: SwapRecordDTO?) {
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