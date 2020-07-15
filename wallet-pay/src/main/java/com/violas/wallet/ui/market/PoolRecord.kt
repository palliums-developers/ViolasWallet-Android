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
import com.palliums.violas.http.MarketPoolRecordDTO.Companion.TYPE_ADD_LIQUIDITY
import com.palliums.violas.http.MarketPoolRecordDTO.Companion.TYPE_REMOVE_LIQUIDITY
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ExchangeManager
import com.violas.wallet.utils.convertViolasTokenUnit
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.item_market_pool_record.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
                PoolDetailsActivity.start(this, it)
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
        //onSuccess.invoke(mockData(), null)
    }

    private suspend fun mockData(): List<MarketPoolRecordDTO> {
        delay(2000)
        return mutableListOf(
            MarketPoolRecordDTO(
                coinA = "VLSUSD",
                amountA = "10000000",
                coinB = "VLSSGD",
                amountB = "13909000",
                liquidityToken = "1.001",
                type = TYPE_ADD_LIQUIDITY,
                version = 1,
                date = System.currentTimeMillis(),
                status = 4001
            ),
            MarketPoolRecordDTO(
                coinA = "VLSUSD",
                amountA = "100000",
                coinB = "VLSEUR",
                amountB = "87770",
                liquidityToken = "0.781",
                type = TYPE_ADD_LIQUIDITY,
                version = 2,
                date = System.currentTimeMillis(),
                status = 4002
            ),
            MarketPoolRecordDTO(
                coinA = "VLSUSD",
                amountA = "10000000",
                coinB = "VLSSGD",
                amountB = "13909000",
                liquidityToken = "1.001",
                type = TYPE_REMOVE_LIQUIDITY,
                version = 3,
                date = System.currentTimeMillis(),
                status = 4001
            ),
            MarketPoolRecordDTO(
                coinA = "VLSUSD",
                amountA = "100000",
                coinB = "VLSEUR",
                amountB = "87770",
                liquidityToken = "0.781",
                type = TYPE_REMOVE_LIQUIDITY,
                version = 4,
                date = System.currentTimeMillis(),
                status = 4002
            )
        )
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

    init {
        itemView.setOnClickListener(this)
    }

    override fun onViewBind(itemPosition: Int, itemData: MarketPoolRecordDTO?) {
        itemData?.let {
            itemView.tvTime.text = formatDate(it.date, simpleDateFormat)
            itemView.tvAToken.text = "${convertViolasTokenUnit(it.amountA)} ${it.coinA}"
            itemView.tvBToken.text = "${convertViolasTokenUnit(it.amountB)} ${it.coinB}"
            itemView.ivIcon.setBackgroundResource(
                getResourceId(
                    if (it.isAddLiquidity())
                        R.attr.iconRecordTypeInput
                    else
                        R.attr.iconRecordTypeOutput,
                    itemView.context
                )
            )
            itemView.tvLiquidityToken.text = getString(
                R.string.market_liquidity_token_record_format,
                "${if (it.isAddLiquidity()) "+" else "-"} ${it.liquidityToken}"
            )

            if (it.status == 4001) {
                itemView.tvState.setText(
                    if (it.isAddLiquidity())
                        R.string.market_pool_add_state_succeeded
                    else
                        R.string.market_pool_remove_state_succeeded
                )
                itemView.tvState.setTextColor(
                    getColorByAttrId(R.attr.textColorSuccess, itemView.context)
                )
            } else {
                itemView.tvState.setText(
                    if (it.isAddLiquidity())
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