package com.violas.wallet.ui.dexOrder.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.palliums.net.LoadState
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.formatDate
import com.palliums.widget.refresh.IRefreshLayout
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.RevokeDexOrderEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.dex.DexOrderTradeDTO
import com.violas.wallet.ui.dexOrder.DexOrderVO
import com.violas.wallet.utils.convertViolasTokenUnit
import com.violas.wallet.widget.dialog.PasswordInputDialog
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

        fun start(context: Context, dexOrderVO: DexOrderVO) {
            val intent = Intent(context, DexOrderDetails2Activity::class.java)
                .apply {
                    putExtra(EXTRA_KEY_DEX_ORDER, dexOrderVO)
                }
            context.startActivity(intent)
        }
    }

    private var dexOrderVO: DexOrderVO? = null
    private lateinit var currentAccount: AccountDO

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

    override fun initViewModel(): PagingViewModel<DexOrderTradeDTO> {
        return DexOrderDetailsViewModel(
            dexOrderVO!!.dto.version
        )
    }

    override fun initViewAdapter(): PagingViewAdapter<DexOrderTradeDTO> {
        return DexOrderDetailsViewAdapter(
            retryCallback = { getViewModel().retry() },
            addHeader = false,
            dexOrderVO = dexOrderVO!!,
            onOpenBrowserView = {
                // TODO violas浏览器暂未实现
                //showToast(R.string.transaction_record_not_supported_query)
            }
        )
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
            dexOrderVO = savedInstanceState.getParcelable(EXTRA_KEY_DEX_ORDER)
        } else if (intent != null) {
            dexOrderVO = intent.getParcelableExtra(EXTRA_KEY_DEX_ORDER)
        }

        if (dexOrderVO == null) {
            return false
        }

        return try {
            currentAccount = AccountManager().currentAccount()

            true
        } catch (e: Exception) {
            false
        }
    }

    private fun initView() {
        setTitle(R.string.title_order_details)

        if (dexOrderVO!!.isOpen() && !dexOrderVO!!.revokedFlag) {
            (getViewModel() as DexOrderDetailsViewModel).loadState.observe(this, Observer {
                when (it.status) {
                    LoadState.Status.RUNNING -> {
                        showProgress()
                    }

                    else -> {
                        dismissProgress()
                    }
                }
            })

            (getViewModel() as DexOrderDetailsViewModel).tipsMessage.observe(this, Observer {
                if (it.isNotEmpty()) {
                    showToast(it)
                }
            })
        }

        getStatusLayout()?.setTipsWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            getString(R.string.tips_no_order_trades)
        )
        getDrawable(R.mipmap.ic_no_transaction_record)?.let {
            getStatusLayout()?.setImageWithStatus(IStatusLayout.Status.STATUS_EMPTY, it)
        }

        mPagingHandler.start()
        initHeaderView(dexOrderVO!!)
    }

    private fun initHeaderView(it: DexOrderVO) {
        // 若拿A换B，A在前B在后
        tvGiveTokenName.text = "${it.giveTokenName} /"
        tvGetTokenName.text = it.getTokenName

        // 若拿A换B，价格、数量、已成交数量均为B的数据
        tvPrice.text = it.getTokenPrice.toString()
        tvTotalAmount.text = convertViolasTokenUnit(it.dto.amountGet)
        tvTradeAmount.text = convertViolasTokenUnit(it.dto.amountFilled)

        tvFee.text = "0.00Vtoken"

        when {
            it.isFinished() -> {
                tvState.setText(
                    if (it.isCanceled())
                        R.string.state_revoked
                    else
                        R.string.state_completed
                )
                tvTime.text = formatDate(it.dto.updateDate)
            }
            it.isOpen() -> {
                tvState.setText(
                    if (it.revokedFlag)
                        R.string.state_revoked
                    else
                        R.string.action_revoke
                )
                tvTime.text = formatDate(it.dto.date)
            }
            else -> {
                tvState.text = ""
                tvTime.text = formatDate(it.dto.updateDate)
            }
        }

        tvState.setOnClickListener(this)
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.tvState -> {

                dexOrderVO?.let { order ->
                    if (order.isOpen() && !order.revokedFlag) {

                        PasswordInputDialog().setConfirmListener { password, dialog ->

                            if (!(getViewModel() as DexOrderDetailsViewModel).revokeOrder(
                                    currentAccount,
                                    password,
                                    order,
                                    onCheckPassword = {
                                        if (it) {
                                            dialog.dismiss()
                                        }
                                    }
                                ) {
                                    order.revokedFlag = true
                                    order.dto.date = System.currentTimeMillis()

                                    tvState.setText(R.string.state_revoked)
                                    tvTime.text = formatDate(order.dto.date)

                                    EventBus.getDefault()
                                        .post(RevokeDexOrderEvent(order.dto.id))
                                }
                            ) {
                                dialog.dismiss()
                            }

                        }.show(this@DexOrderDetails2Activity.supportFragmentManager)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        dexOrderVO?.let {
            outState.putParcelable(EXTRA_KEY_DEX_ORDER, it)
        }
    }
}