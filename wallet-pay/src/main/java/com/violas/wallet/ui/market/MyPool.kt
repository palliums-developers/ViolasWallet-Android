package com.violas.wallet.ui.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.palliums.base.BaseViewHolder
import com.palliums.listing.ListingViewAdapter
import com.palliums.listing.ListingViewModel
import com.palliums.utils.getString
import com.palliums.widget.refresh.IRefreshLayout
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BaseListingActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.event.MarketPageType
import com.violas.wallet.event.SwitchMarketPageEvent
import com.violas.wallet.event.SwitchMarketPoolOpModeEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.exchange.PoolLiquidityDTO
import com.violas.wallet.ui.main.market.pool.MarketPoolOpMode
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.activity_my_pool.*
import kotlinx.android.synthetic.main.item_my_pool_liquidity_token.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

/**
 * Created by elephant on 2020/7/15 16:36.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 我的资金池页面
 */
class MyPoolActivity : BaseListingActivity<PoolLiquidityDTO>() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_my_pool
    }

    override fun enableRefresh(): Boolean {
        return true
    }

    override fun getRecyclerView(): RecyclerView {
        return recyclerView
    }

    override fun getRefreshLayout(): IRefreshLayout? {
        return refreshLayout
    }

    override fun getStatusLayout(): IStatusLayout? {
        return statusLayout
    }

    override fun lazyInitListingViewModel(): ListingViewModel<PoolLiquidityDTO> {
        return ViewModelProvider(this).get(MyPoolViewModel::class.java)
    }

    override fun lazyInitListingViewAdapter(): ListingViewAdapter<PoolLiquidityDTO> {
        return ViewAdapter()
    }

    fun getViewModel(): MyPoolViewModel {
        return getListingViewModel() as MyPoolViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.my_pool_title)

        btnTransferIn.setOnClickListener {
            EventBus.getDefault().post(SwitchMarketPageEvent(MarketPageType.Pool))
            EventBus.getDefault()
                .postSticky(SwitchMarketPoolOpModeEvent(MarketPoolOpMode.TransferIn))
            close()
        }

        btnTransferOut.setOnClickListener {
            EventBus.getDefault().post(SwitchMarketPageEvent(MarketPageType.Pool))
            EventBus.getDefault()
                .postSticky(SwitchMarketPoolOpModeEvent(MarketPoolOpMode.TransferOut))
            close()
        }

        getListingHandler().init()
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

                    initView()
                }
            })
    }

    private fun initNotLoginView() {
        refreshLayout.setEnableRefresh(false)
        refreshLayout.setEnableLoadMore(false)
        statusLayout.showStatus(IStatusLayout.Status.STATUS_EMPTY)
    }

    private fun initView() {
        refreshLayout.setOnRefreshListener {
            getViewModel().execute()
        }

        getViewModel().liquidityTotalAmount.observe(this, Observer {
            tvLiquidityTotalAmount.text = convertAmountToDisplayAmountStr(it)
        })

        getViewModel().execute()
    }

    class ViewAdapter : ListingViewAdapter<PoolLiquidityDTO>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<PoolLiquidityDTO> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_my_pool_liquidity_token,
                    parent,
                    false
                )
            )
        }
    }

    class ViewHolder(
        view: View
    ) : BaseViewHolder<PoolLiquidityDTO>(view) {

        override fun onViewBind(itemPosition: Int, itemData: PoolLiquidityDTO?) {
            itemData?.let {
                itemView.tvLiquidityAmount.text = getString(
                    R.string.market_common_label_pool_token_amount_format,
                    convertAmountToDisplayAmountStr(it.amount)
                )
                itemView.tvTokenA.text =
                    "${convertAmountToDisplayAmountStr(it.coinA.amount)} ${it.coinA.displayName}"
                itemView.tvTokenB.text =
                    "${convertAmountToDisplayAmountStr(it.coinB.amount)} ${it.coinB.displayName}"
            }
        }
    }
}

class MyPoolViewModel : ListingViewModel<PoolLiquidityDTO>() {

    val liquidityTotalAmount = MutableLiveData<String>()

    private lateinit var address: String

    private val exchangeService by lazy {
        DataRepository.getExchangeService()
    }

    suspend fun initAddress() = withContext(Dispatchers.IO) {
        val violasAccount =
            AccountManager().getIdentityByCoinType(getViolasCoinType().coinNumber())
                ?: return@withContext false

        address = violasAccount.address
        return@withContext true
    }

    override suspend fun loadData(vararg params: Any): List<PoolLiquidityDTO> {
        val userPoolInfo = exchangeService.getUserPoolInfo(address)

        liquidityTotalAmount.postValue(userPoolInfo?.liquidityTotalAmount ?: "0")

        return userPoolInfo?.liquidityList ?: emptyList()
    }
}