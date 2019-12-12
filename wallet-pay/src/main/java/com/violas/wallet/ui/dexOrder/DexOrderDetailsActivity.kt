package com.violas.wallet.ui.dexOrder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import com.palliums.net.LoadState
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.RevokeDexOrderEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.widget.dialog.PasswordInputDialog
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
class DexOrderDetailsActivity : BasePagingActivity<DexOrderVO>() {

    companion object {
        private const val EXTRA_KEY_DEX_ORDER = "EXTRA_KEY_DEX_ORDER"

        fun start(context: Context, dexOrderVO: DexOrderVO) {
            val intent = Intent(context, DexOrderDetailsActivity::class.java)
                .apply {
                    putExtra(EXTRA_KEY_DEX_ORDER, dexOrderVO)
                }
            context.startActivity(intent)
        }
    }

    private var dexOrderDO: DexOrderVO? = null
    private lateinit var currentAccount: AccountDO

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
            addHeader = true,
            onOpenBrowserView = {
                // TODO violas浏览器暂未实现
                //showToast(R.string.transaction_record_not_supported_query)
            },
            onClickRevokeOrder = { dexOrder, position ->

                PasswordInputDialog().setConfirmListener { password, dialog ->

                    if (!(getViewModel() as DexOrderViewModel).revokeOrder(
                            currentAccount,
                            password,
                            dexOrder,
                            onCheckPassword = {
                                if (it) {
                                    dialog.dismiss()
                                }
                            }
                        ) {
                            dexOrder.revokedFlag = true
                            dexOrder.dexOrderDTO.date = System.currentTimeMillis()

                            getViewAdapter().notifyItemChanged(position)

                            EventBus.getDefault().post(RevokeDexOrderEvent(dexOrder.dexOrderDTO.id))
                        }
                    ) {
                        dialog.dismiss()
                    }

                }.show(this@DexOrderDetailsActivity.supportFragmentManager)
            }
        ).apply { headerData = dexOrderDO }
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
            dexOrderDO = savedInstanceState.getParcelable(EXTRA_KEY_DEX_ORDER)
        } else if (intent != null) {
            dexOrderDO = intent.getParcelableExtra(EXTRA_KEY_DEX_ORDER)
        }

        if (dexOrderDO == null) {
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        dexOrderDO?.let {
            outState.putParcelable(EXTRA_KEY_DEX_ORDER, dexOrderDO)
        }
    }
}