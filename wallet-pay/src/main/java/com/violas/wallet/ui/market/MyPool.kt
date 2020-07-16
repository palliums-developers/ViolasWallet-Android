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
import com.palliums.violas.http.LiquidityTokenDTO
import com.palliums.widget.refresh.IRefreshLayout
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseListingActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.MarketPageType
import com.violas.wallet.event.SwitchMarketPageEvent
import com.violas.wallet.event.SwitchMarketPoolOpModeEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.main.market.pool.MarketPoolOpMode
import com.violas.wallet.utils.convertViolasTokenUnit
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
class MyPoolActivity : BaseListingActivity<LiquidityTokenDTO>() {

    private val viewModel by lazy {
        ViewModelProvider(this).get(MyPoolViewModel::class.java)
    }
    private val viewAdapter by lazy {
        MyPoolViewAdapter()
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_my_pool
    }

    override fun loadingUseDialog(): Boolean {
        return false
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

    override fun getViewModel(): ListingViewModel<LiquidityTokenDTO> {
        return viewModel
    }

    override fun getViewAdapter(): ListingViewAdapter<LiquidityTokenDTO> {
        return viewAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.title_market_my_pool)

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
            viewModel.execute()
        }

        viewModel.totalLiquidityAmount.observe(this, Observer {
            tvTotalLiquidityAmount.text = it
        })

        statusLayout.showStatus(IStatusLayout.Status.STATUS_LOADING)
        viewModel.execute()
    }
}

class MyPoolViewModel : ListingViewModel<LiquidityTokenDTO>() {

    val totalLiquidityAmount = MutableLiveData<String>()

    private lateinit var address: String

    private val violasService by lazy { DataRepository.getViolasService() }

    suspend fun initAddress() = withContext(Dispatchers.IO) {
        val violasAccount =
            AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
                ?: return@withContext false

        address = violasAccount.address
        return@withContext true
    }

    override suspend fun loadData(vararg params: Any): List<LiquidityTokenDTO> {
        val userPoolInfo = violasService.getUserPoolInfo(address)

        totalLiquidityAmount.postValue(userPoolInfo?.totalLiquidityAmount ?: "0")

        return userPoolInfo?.liquidityTokens ?: emptyList()
    }

}

class MyPoolViewAdapter : ListingViewAdapter<LiquidityTokenDTO>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<LiquidityTokenDTO> {
        return MyPoolViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_my_pool_liquidity_token,
                parent,
                false
            )
        )
    }
}

class MyPoolViewHolder(
    view: View
) : BaseViewHolder<LiquidityTokenDTO>(view) {

    override fun onViewBind(itemPosition: Int, itemData: LiquidityTokenDTO?) {
        itemData?.let {
            itemView.tvLiquidityAmount.text = getString(
                R.string.market_liquidity_token_amount_format, it.amount
            )
            itemView.tvTokenA.text = "${convertViolasTokenUnit(it.coinAAmount)} ${it.coinAName}"
            itemView.tvTokenB.text = "${convertViolasTokenUnit(it.coinBAmount)} ${it.coinBName}"
        }
    }
}