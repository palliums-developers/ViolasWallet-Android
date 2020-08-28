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
import com.violas.wallet.repository.http.exchange.PoolRecordDTO.Companion.TYPE_ADD_LIQUIDITY
import com.violas.wallet.repository.http.exchange.PoolRecordDTO.Companion.TYPE_REMOVE_LIQUIDITY
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.getAmountPrefix
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.item_market_pool_record.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    private val viewModel by lazy {
        ViewModelProvider(this).get(PoolRecordViewModel::class.java)
    }
    private val viewAdapter by lazy {
        PoolRecordViewAdapter {
            PoolDetailsActivity.start(this, it)
        }
    }

    override fun getViewModel(): PagingViewModel<PoolRecordDTO> {
        return viewModel
    }

    override fun getViewAdapter(): PagingViewAdapter<PoolRecordDTO> {
        return viewAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.title_market_pool_records)
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
        //onSuccess.invoke(mockData(), null)
    }

    private suspend fun mockData(): List<PoolRecordDTO> {
        delay(2000)
        return mutableListOf(
            PoolRecordDTO(
                coinAName = "VLSUSD",
                coinAAmount = "10000000",
                coinBName = "VLSSGD",
                coinBAmount = "13909000",
                liquidityAmount = "1.001",
                gasCoinAmount = "",
                gasCoinName = "",
                type = TYPE_ADD_LIQUIDITY,
                time = System.currentTimeMillis(),
                confirmedTime = System.currentTimeMillis() + 1000,
                version = 1,
                status = 4001
            ),
            PoolRecordDTO(
                coinAName = "VLSUSD",
                coinAAmount = "100000",
                coinBName = "VLSEUR",
                coinBAmount = "87770",
                liquidityAmount = "0.781",
                gasCoinAmount = "",
                gasCoinName = "",
                type = TYPE_ADD_LIQUIDITY,
                time = System.currentTimeMillis(),
                confirmedTime = System.currentTimeMillis() + 1000,
                version = 2,
                status = 4002
            ),
            PoolRecordDTO(
                coinAName = "VLSUSD",
                coinAAmount = "10000000",
                coinBName = "VLSSGD",
                coinBAmount = "13909000",
                liquidityAmount = "1.001",
                gasCoinAmount = "",
                gasCoinName = "",
                type = TYPE_REMOVE_LIQUIDITY,
                time = System.currentTimeMillis(),
                confirmedTime = System.currentTimeMillis() + 1000,
                version = 3,
                status = 4001
            ),
            PoolRecordDTO(
                coinAName = "VLSUSD",
                coinAAmount = "100000",
                coinBName = "VLSEUR",
                coinBAmount = "87770",
                liquidityAmount = "0.781",
                gasCoinAmount = "",
                gasCoinName = "",
                type = TYPE_REMOVE_LIQUIDITY,
                time = System.currentTimeMillis(),
                confirmedTime = System.currentTimeMillis() + 1000,
                version = 4,
                status = 4002
            )
        )
    }
}

class PoolRecordViewAdapter(
    private val clickItemCallback: ((PoolRecordDTO) -> Unit)? = null
) : PagingViewAdapter<PoolRecordDTO>() {

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
                    getString(R.string.value_null)
                } else {
                    "${convertAmountToDisplayAmountStr(it.coinAAmount)} ${it.coinAName}"
                }

            itemView.tvBToken.text =
                if (it.coinBName.isNullOrBlank() || it.coinBAmount.isNullOrBlank()) {
                    getString(R.string.value_null)
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
                    getString(R.string.value_null)
                } else {
                    val amount = BigDecimal(it.liquidityAmount)
                    getString(
                        R.string.market_liquidity_token_amount_format,
                        "${getAmountPrefix(
                            amount,
                            it.isAddLiquidity()
                        )}${convertAmountToDisplayAmountStr(amount)}"
                    )
                }

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