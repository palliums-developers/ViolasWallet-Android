package com.violas.wallet.ui.dexOrder.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.palliums.net.LoadState
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.formatDate
import com.palliums.widget.refresh.IRefreshLayout
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.RevokeDexOrderEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.dex.DexOrderDTO
import com.violas.wallet.repository.http.dex.DexOrderTradeDTO
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertViolasTokenPrice
import com.violas.wallet.utils.convertViolasTokenUnit
import kotlinx.android.synthetic.main.activity_dex_order_details.*
import kotlinx.android.synthetic.main.item_dex_order_details_header.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

/**
 * Created by elephant on 2019-12-09 11:34.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易中心订单详情页面
 */
class DexOrderDetails2Activity : BasePagingActivity<DexOrderTradeDTO>() {

    companion object {
        private const val EXTRA_KEY_DEX_ORDER = "EXTRA_KEY_DEX_ORDER"

        fun start(context: Context, dexOrder: DexOrderDTO) {
            val intent = Intent(context, DexOrderDetails2Activity::class.java)
                .apply {
                    putExtra(EXTRA_KEY_DEX_ORDER, dexOrder)
                }
            context.startActivity(intent)
        }
    }

    private var dexOrder: DexOrderDTO? = null
    private lateinit var currentAccount: AccountDO
    private val mAccountManager by lazy { AccountManager() }

    private val mViewModel by viewModels<DexOrderDetailsViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return DexOrderDetailsViewModel(
                    dexOrder!!.version
                ) as T
            }
        }
    }

    private val mViewAdapter by lazy {
        DexOrderDetailsViewAdapter(
            addHeader = false,
            dexOrder = dexOrder!!,
            onOpenBrowserView = {
                // TODO violas浏览器暂未实现
                //showToast(R.string.transaction_record_not_supported_query)
            }
        )
    }

    override fun getViewModel(): PagingViewModel<DexOrderTradeDTO> {
        return mViewModel
    }

    override fun getViewAdapter(): PagingViewAdapter<DexOrderTradeDTO> {
        return mViewAdapter
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_dex_order_details
    }

    override fun getRecyclerView(): RecyclerView {
        return rvRecyclerView
    }

    override fun getRefreshLayout(): IRefreshLayout? {
        return rlRefreshLayout
    }

    override fun getStatusLayout(): IStatusLayout? {
        return slStatusLayout
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launch(Dispatchers.IO) {
            val result = initData(savedInstanceState)
            withContext(Dispatchers.Main) {
                if (result) {
                    initView()
                } else {
                    finish()
                }
            }
        }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {

        if (savedInstanceState != null) {
            dexOrder = savedInstanceState.getParcelable(EXTRA_KEY_DEX_ORDER)
        } else if (intent != null) {
            dexOrder = intent.getParcelableExtra(EXTRA_KEY_DEX_ORDER)
        }

        if (dexOrder == null) {
            return false
        }

        return try {
            currentAccount = mAccountManager.currentAccount()

            true
        } catch (e: Exception) {
            false
        }
    }

    private fun initView() {
        setTitle(R.string.title_order_details)

        if (dexOrder!!.isOpen()) {
            mViewModel.loadState.observe(this, Observer {
                when (it.peekData().status) {
                    LoadState.Status.RUNNING -> {
                        showProgress()
                    }

                    else -> {
                        dismissProgress()
                    }
                }
            })

            mViewModel.tipsMessage.observe(this, Observer {
                it.getDataIfNotHandled()?.let { msg ->
                    if (msg.isNotEmpty()) {
                        showToast(msg)
                    }
                }
            })
        }

        mPagingHandler.init()
        getStatusLayout()?.setTipsWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            getString(R.string.tips_no_order_trades)
        )

        mPagingHandler.start()
        initHeaderView(dexOrder!!)
    }

    private fun initHeaderView(it: DexOrderDTO) {
        // 若拿A换B，A在前B在后
        tvGiveTokenName.text = "${it.tokenGiveSymbol} /"
        tvGetTokenName.text = it.tokenGetSymbol

        // 若拿A换B，价格、数量、已成交数量均为B的数据
        tvPrice.text = convertViolasTokenPrice(it.tokenGetPrice.toString())
        tvTotalLiquidityAmount.text = convertViolasTokenUnit(it.amountGet)
        tvTradeAmount.text = convertViolasTokenUnit(it.amountFilled)

        tvFee.text = "0 ${CoinTypes.Violas.coinUnit()}"
        tvTime.text = formatDate(it.updateDate)

        when {
            it.isFinished() -> {
                tvState.setText(
                    if (it.isCanceled())
                        R.string.state_revoked
                    else
                        R.string.state_completed
                )
            }
            it.isUnfinished() -> {
                tvState.setText(
                    if (it.isOpen())
                        R.string.action_revoke
                    else
                        R.string.state_revoking
                )
            }
            else -> {
                tvState.text = ""
            }
        }

        tvState.setOnClickListener(this)
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.tvState -> {
                dexOrder?.let { dexOrder ->
                    if (dexOrder.isOpen()) {
                        authenticateAccount(currentAccount, mAccountManager) {
                            mViewModel.revokeOrder(it, dexOrder) {
                                dexOrder.updateStateToRevoking()
                                initHeaderView(dexOrder)

                                EventBus.getDefault().post(
                                    RevokeDexOrderEvent(
                                        dexOrder.id,
                                        dexOrder.updateDate
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        dexOrder?.let {
            outState.putParcelable(EXTRA_KEY_DEX_ORDER, it)
        }
    }
}