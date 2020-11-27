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
class SwapRecordActivity : BasePagingActivity<SwapRecordDTO>() {

    override fun lazyInitPagingViewModel(): PagingViewModel<SwapRecordDTO> {
        return ViewModelProvider(this).get(SwapRecordViewModel::class.java)
    }

    override fun lazyInitPagingViewAdapter(): PagingViewAdapter<SwapRecordDTO> {
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

        setTitle(R.string.title_market_swap_records)
        getPagingHandler().init()
        WalletAppViewModel.getViewModelInstance().mExistsAccountLiveData
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
        private val clickItemCallback: ((SwapRecordDTO) -> Unit)? = null,
        private val clickRetryCallback: ((SwapRecordDTO, Int) -> Unit)? = null
    ) : PagingViewAdapter<SwapRecordDTO>() {

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
        private val clickItemCallback: ((SwapRecordDTO) -> Unit)? = null,
        private val clickRetryCallback: ((SwapRecordDTO, Int) -> Unit)? = null
    ) : BaseViewHolder<SwapRecordDTO>(view) {

        init {
            itemView.setOnClickListener(this)
            itemView.tvRetry.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: SwapRecordDTO?) {
            itemData?.let {
                itemView.tvTime.text = formatDate(it.time, simpleDateFormat)

                itemView.tvInputCoin.text =
                    if (it.inputCoinDisplayName.isNullOrBlank() || it.inputCoinAmount.isNullOrBlank()) {
                        getString(R.string.value_null)
                    } else {
                        "${
                            convertAmountToDisplayAmountStr(
                                it.inputCoinAmount,
                                str2CoinType(it.inputChainName)
                            )
                        } ${it.inputCoinDisplayName}"
                    }

                itemView.tvOutputCoin.text =
                    if (it.outputCoinDisplayName.isNullOrBlank() || it.outputCoinAmount.isNullOrBlank()) {
                        getString(R.string.value_null)
                    } else {
                        "${
                            convertAmountToDisplayAmountStr(
                                it.outputCoinAmount,
                                str2CoinType(it.outputChainName)
                            )
                        } ${it.outputCoinDisplayName}"
                    }

                when (it.status) {
                    4001 -> {
                        itemView.tvRetry.visibility = View.GONE
                        itemView.tvState.setText(R.string.market_swap_state_succeeded)
                        itemView.tvState.setTextColor(
                            getColorByAttrId(R.attr.textColorSuccess, itemView.context)
                        )
                    }

                    4002 -> {
                        // TODO 取消先隐藏
                        itemView.tvRetry.visibility = View.GONE
                        itemView.tvRetry.expandTouchArea()
                        itemView.tvState.setText(R.string.market_swap_state_processing)
                        itemView.tvState.setTextColor(
                            getColorByAttrId(R.attr.textColorProcessing, itemView.context)
                        )
                    }

                    4004 -> {
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
}

class SwapRecordViewModel : PagingViewModel<SwapRecordDTO>() {

    private val exchangeService by lazy {
        DataRepository.getExchangeService()
    }

    private lateinit var violasWalletAddress: String
    private lateinit var libraWalletAddress: String
    private lateinit var bitcoinWalletAddress: String

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

        violasWalletAddress = violasAccount.address
        libraWalletAddress = libraAccount.address
        bitcoinWalletAddress = bitcoinAccount.address
        return@withContext true
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<SwapRecordDTO>, Any?) -> Unit
    ) {
        val list = exchangeService.getSwapRecords(
            violasWalletAddress,
            libraWalletAddress,
            bitcoinWalletAddress,
            pageSize,
            (pageNumber - 1) * pageSize
        )
        onSuccess.invoke(list, null)
    }
}