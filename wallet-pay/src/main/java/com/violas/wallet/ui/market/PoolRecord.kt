package com.violas.wallet.ui.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.*
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.exchange.PoolRecordDTO
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.getAmountPrefix
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.item_market_pool_record.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2020/7/14 18:22.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易市场资金池记录页面
 */
class PoolRecordActivity : BasePagingActivity<PoolRecordDTO>() {

    override fun lazyInitPagingViewModel(): PagingViewModel<PoolRecordDTO> {
        return ViewModelProvider(this).get(PoolRecordViewModel::class.java)
    }

    override fun lazyInitPagingViewAdapter(): PagingViewAdapter<PoolRecordDTO> {
        return ViewAdapter {
            PoolDetailsActivity.start(this, it)
        }
    }

    fun getViewModel(): PoolRecordViewModel {
        return getPagingViewModel() as PoolRecordViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.pool_records_title)
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
        private val clickItemCallback: ((PoolRecordDTO) -> Unit)? = null
    ) : PagingViewAdapter<PoolRecordDTO>() {

        private val simpleDateFormat = SimpleDateFormat("MM.dd HH:mm:ss", Locale.ENGLISH)

        override fun onCreateViewHolderSupport(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<out Any> {
            return ViewHolder(
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

    class ViewHolder(
        view: View,
        private val simpleDateFormat: SimpleDateFormat,
        private val clickItemCallback: ((PoolRecordDTO) -> Unit)? = null
    ) : BaseViewHolder<PoolRecordDTO>(view) {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: PoolRecordDTO?) {
            itemData?.let {
                itemView.tvTime.text = formatDate(
                    correctDateLength(it.confirmedTime) - 1000,
                    simpleDateFormat
                )

                itemView.tvAToken.text =
                    if (it.coinAName.isNullOrBlank() || it.coinAAmount.isNullOrBlank()) {
                        getString(R.string.common_desc_value_null)
                    } else {
                        "${convertAmountToDisplayAmountStr(it.coinAAmount)} ${it.coinAName}"
                    }

                itemView.tvBToken.text =
                    if (it.coinBName.isNullOrBlank() || it.coinBAmount.isNullOrBlank()) {
                        getString(R.string.common_desc_value_null)
                    } else {
                        "${convertAmountToDisplayAmountStr(it.coinBAmount)} ${it.coinBName}"
                    }

                itemView.ivIcon.setBackgroundResource(
                    getResourceId(
                        if (it.isAddLiquidity())
                            R.attr.iconRecordTypeInput
                        else
                            R.attr.iconRecordTypeOutput,
                        itemView.context
                    )
                )

                itemView.tvLiquidity.text =
                    if (it.liquidityAmount.isNullOrBlank()) {
                        getString(R.string.common_desc_value_null)
                    } else {
                        val amount = BigDecimal(it.liquidityAmount)
                        getString(
                            R.string.market_common_label_pool_token_amount_format,
                            "${
                                getAmountPrefix(
                                    amount,
                                    it.isAddLiquidity()
                                )
                            }${convertAmountToDisplayAmountStr(amount)}"
                        )
                    }

                if (it.isSuccess()) {
                    itemView.tvState.setText(
                        if (it.isAddLiquidity())
                            R.string.pool_txn_state_add_liquidity_succeeded
                        else
                            R.string.pool_txn_state_remove_liquidity_succeeded
                    )
                    itemView.tvState.setTextColor(
                        getColorByAttrId(R.attr.textColorSuccess, itemView.context)
                    )
                } else {
                    itemView.tvState.setText(
                        if (it.isAddLiquidity())
                            R.string.pool_txn_state_add_liquidity_failed
                        else
                            R.string.pool_txn_state_remove_liquidity_failed
                    )
                    itemView.tvState.setTextColor(
                        getColorByAttrId(R.attr.textColorFailure, itemView.context)
                    )
                }
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: PoolRecordDTO?) {
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

class PoolRecordViewModel : PagingViewModel<PoolRecordDTO>() {

    private val exchangeService by lazy { DataRepository.getExchangeService() }

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
        onSuccess: (List<PoolRecordDTO>, Any?) -> Unit
    ) {
        val swapRecords =
            exchangeService.getPoolRecords(
                address, pageSize, (pageNumber - 1) * pageSize
            )
        onSuccess.invoke(swapRecords ?: emptyList(), null)
    }
}