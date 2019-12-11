package com.violas.wallet.ui.dexOrder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.palliums.net.LoadState
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.widget.refresh.IRefreshLayout
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.violas.wallet.event.RevokeDexOrderEvent
import com.violas.wallet.widget.dialog.PasswordInputDialog
import kotlinx.android.synthetic.main.activity_dex_order_details.*
import kotlinx.android.synthetic.main.item_dex_order_details_header.*
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2019-12-09 11:34.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易中心订单详情页面
 */
class DexOrderDetails2Activity : BasePagingActivity<DexOrderVO>() {

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

    private var dexOrderDO: DexOrderVO? = null

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

    override fun initViewModel(): PagingViewModel<DexOrderVO> {
        return DexOrderViewModel(
            accountAddress = dexOrderDO!!.dexOrderDTO.user,
            orderState = null,
            giveTokenAddress = dexOrderDO!!.dexOrderDTO.tokenGive,
            getTokenAddress = dexOrderDO!!.dexOrderDTO.tokenGet
        )
    }

    override fun initViewAdapter(): PagingViewAdapter<DexOrderVO> {
        return DexOrderViewAdapter(
            retryCallback = { getViewModel().retry() },
            showOrderDetails = true,
            onOpenBrowserView = {
                // TODO violas浏览器暂未实现
                //showToast(R.string.transaction_record_not_supported_query)
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            dexOrderDO = savedInstanceState.getParcelable(EXTRA_KEY_DEX_ORDER)
        } else if (intent != null) {
            dexOrderDO = intent.getParcelableExtra(EXTRA_KEY_DEX_ORDER)
        }

        if (dexOrderDO == null) {
            finish()
            return
        }

        setTitle(R.string.title_order_details)

        (getViewModel() as DexOrderViewModel).loadState.observe(this, Observer {
            when (it.status) {
                LoadState.Status.RUNNING -> {
                    showProgress()
                }

                else -> {
                    dismissProgress()
                }
            }
        })

        (getViewModel() as DexOrderViewModel).tipsMessage.observe(this, Observer {
            if (it.isNotEmpty()) {
                showToast(it)
            }
        })

        mPagingHandler.start()
        initHeaderView(dexOrderDO!!)
    }

    private fun initHeaderView(it: DexOrderVO) {
        // 若拿A换B，A在前B在后
        tvGiveTokenName.text = "${it.giveTokenName} /"
        tvGetTokenName.text = it.getTokenName

        // 若拿A换B，价格、数量、已成交数量均为B的数据
        tvPrice.text = it.getTokenPrice.toString()
        tvTotalAmount.text = it.dexOrderDTO.amountGet
        tvTradeAmount.text = it.dexOrderDTO.amountFilled

        tvFee.text = "0.00Vtoken"

        val simpleDateFormat = SimpleDateFormat("MM.dd HH:mm:ss", Locale.ENGLISH)
        when {
            it.isFinished() -> {
                tvState.setText(
                    if (it.isCanceled())
                        R.string.state_revoked
                    else
                        R.string.state_completed
                )
                tvTime.text = simpleDateFormat.format(it.dexOrderDTO.updateDate)
            }
            it.isOpen() -> {
                tvState.setText(
                    if (it.revokedFlag)
                        R.string.state_revoked
                    else
                        R.string.action_revoke
                )
                tvTime.text = simpleDateFormat.format(it.dexOrderDTO.date)
            }
            else -> {
                tvState.text = ""
                tvTime.text = simpleDateFormat.format(it.dexOrderDTO.updateDate)
            }
        }

        tvState.setOnClickListener(this)
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.tvState -> {

                dexOrderDO?.let { dexOrder ->

                    if (dexOrder.isOpen() && !dexOrder.revokedFlag) {

                        PasswordInputDialog().setConfirmListener { password, dialog ->
                            dialog.dismiss()

                            (getViewModel() as DexOrderViewModel).revokeOrder(password, dexOrder) {
                                dexOrder.revokedFlag = true
                                dexOrder.dexOrderDTO.date = System.currentTimeMillis()

                                tvState.setText(R.string.state_revoked)
                                tvTime.text = SimpleDateFormat(
                                    "MM.dd HH:mm:ss",
                                    Locale.ENGLISH
                                ).format(dexOrder.dexOrderDTO.date)

                                EventBus.getDefault()
                                    .post(RevokeDexOrderEvent(dexOrder.dexOrderDTO.id))
                            }

                        }.show(this@DexOrderDetails2Activity.supportFragmentManager)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        dexOrderDO?.let {
            outState.putParcelable(EXTRA_KEY_DEX_ORDER, dexOrderDO)
        }
    }
}