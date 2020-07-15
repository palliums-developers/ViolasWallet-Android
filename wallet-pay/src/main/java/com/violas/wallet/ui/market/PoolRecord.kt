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
import com.palliums.utils.getResourceId
import com.palliums.utils.getString
import com.palliums.violas.http.MarketPoolRecordDTO
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ExchangeManager
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.item_market_pool_record.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2020/7/14 18:22.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易市场资金池记录页面
 */
class PoolRecordActivity : BasePagingActivity<MarketPoolRecordDTO>() {

    private val viewModel by lazy {
        ViewModelProvider(this).get(PoolRecordViewModel::class.java)
    }
    private val viewAdapter by lazy {
        PoolRecordViewAdapter(
            retryCallback = { viewModel.retry() },
            clickItemCallback = {
                // TODO 进入资金池详情页面
            }
        )
    }

    override fun getViewModel(): PagingViewModel<MarketPoolRecordDTO> {
        return viewModel
    }

    override fun getViewAdapter(): PagingViewAdapter<MarketPoolRecordDTO> {
        return viewAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.fund_pool_records)
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

class PoolRecordViewModel : PagingViewModel<MarketPoolRecordDTO>() {

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
        onSuccess: (List<MarketPoolRecordDTO>, Any?) -> Unit
    ) {
        val swapRecords =
            exchangeManager.mViolasService.getMarketPoolRecords(
                address, pageSize, (pageNumber - 1) * pageSize
            )
        onSuccess.invoke(swapRecords ?: emptyList(), null)
    }
}

class PoolRecordViewAdapter(
    retryCallback: () -> Unit,
    private val clickItemCallback: ((MarketPoolRecordDTO) -> Unit)? = null
) : PagingViewAdapter<MarketPoolRecordDTO>(retryCallback, PoolRecordDiffCallback()) {

    private val simpleDateFormat = SimpleDateFormat("MM.dd HH:mm:ss", Locale.ENGLISH)

    override fun onCreateViewHolderSupport(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<out Any> {
        return PoolRecordViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_market_pool_record,
                parent,
                false
            ),
            simpleDateFormat,
            clickItemCallback
        )
    }
}

class PoolRecordViewHolder(
    view: View,
    private val simpleDateFormat: SimpleDateFormat,
    private val clickItemCallback: ((MarketPoolRecordDTO) -> Unit)? = null
) : BaseViewHolder<MarketPoolRecordDTO>(view) {

    companion object {
        private const val TYPE_ADD_LIQUIDITY = "ADD_LIQUIDITY"
        private const val TYPE_REMOVE_LIQUIDITY = "REMOVE_LIQUIDITY"
    }

    init {
        itemView.setOnClickListener(this)
    }

    override fun onViewBind(itemPosition: Int, itemData: MarketPoolRecordDTO?) {
        itemData?.let {
            itemView.tvTime.text = formatDate(it.date, simpleDateFormat)
            itemView.tvAToken.text = "${it.amountA} ${it.coinA}"
            itemView.tvBToken.text = "${it.amountB} ${it.coinB}"
            itemView.ivIcon.setBackgroundColor(
                getResourceId(
                    if (it.type.equals(TYPE_ADD_LIQUIDITY, true))
                        R.attr.iconRecordTypeInput
                    else
                        R.attr.iconRecordTypeOutput,
                    itemView.context
                )
            )
            itemView.tvLiquidityToken.text = getString(
                R.string.market_liquidity_token_record_format,
                if (it.type.equals(TYPE_ADD_LIQUIDITY, true))
                    "+${it.token}"
                else
                    "-${it.token}"
            )

            if (it.status == 4001) {
                itemView.tvState.setText(
                    if (it.type.equals(TYPE_ADD_LIQUIDITY, true))
                        R.string.market_pool_add_state_succeeded
                    else
                        R.string.market_pool_remove_state_succeeded
                )
                itemView.tvState.setTextColor(
                    getColorByAttrId(R.attr.textColorSuccess, itemView.context)
                )
            } else {
                itemView.tvState.setText(
                    if (it.type.equals(TYPE_ADD_LIQUIDITY, true))
                        R.string.market_pool_add_state_failed
                    else
                        R.string.market_pool_remove_state_failed
                )
                itemView.tvState.setTextColor(
                    getColorByAttrId(R.attr.textColorFailure, itemView.context)
                )
            }
        }
    }

    override fun onViewClick(view: View, itemPosition: Int, itemData: MarketPoolRecordDTO?) {
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

class PoolRecordDiffCallback : DiffUtil.ItemCallback<MarketPoolRecordDTO>() {
    override fun areItemsTheSame(
        oldItem: MarketPoolRecordDTO,
        newItem: MarketPoolRecordDTO
    ): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }

    override fun areContentsTheSame(
        oldItem: MarketPoolRecordDTO,
        newItem: MarketPoolRecordDTO
    ): Boolean {
        return oldItem == newItem
    }
}