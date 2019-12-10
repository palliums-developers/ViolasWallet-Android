package com.violas.wallet.ui.dexOrder

import android.os.Bundle
import androidx.annotation.StringDef
import androidx.lifecycle.Observer
import com.palliums.net.LoadState
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.violas.wallet.base.BasePagingFragment
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.widget.dialog.PasswordInputDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2019-12-06 12:02.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易中心订单
 */

@StringDef(
    DexOrdersState.OPEN,
    DexOrdersState.FILLED,
    DexOrdersState.CANCELED,
    DexOrdersState.FINISHED
)
annotation class DexOrdersState {
    companion object {
        const val OPEN = "0"        // open
        const val FILLED = "1"      // filled
        const val CANCELED = "2"    // canceled
        const val FINISHED = "3"    // finished（filled and canceled）
    }
}

class DexOrdersFragment : BasePagingFragment<DexOrderVO>() {

    companion object {
        private const val EXTRA_KEY_ORDER_STATE = "EXTRA_KEY_ORDER_STATE"

        /**
         * @param orderState 订单状态，若为null则查询所有
         */
        fun newInstance(
            @DexOrdersState
            orderState: String?
        ): DexOrdersFragment {
            val bundle = Bundle().apply {
                orderState?.let { putString(EXTRA_KEY_ORDER_STATE, it) }
            }

            return DexOrdersFragment().apply {
                arguments = bundle
            }
        }
    }

    @DexOrdersState
    private var orderState: String? = null
    private lateinit var accountAddress: String

    override fun initViewModel(): PagingViewModel<DexOrderVO> {
        return DexOrderViewModel(
            accountAddress = accountAddress,
            orderState = orderState,
            giveTokenAddress = null,
            getTokenAddress = null
        )
    }

    override fun initViewAdapter(): PagingViewAdapter<DexOrderVO> {
        return DexOrderViewAdapter(
            retryCallback = { getViewModel().retry() },
            showOrderDetails = false,
            viewModel = getViewModel() as DexOrderViewModel,
            onOpenOrderDetails = {
                DexOrderDetailsActivity.start(requireContext(), it)
            },
            onOpenBrowserView = {
                // TODO violas浏览器暂未实现
                //showToast(R.string.transaction_record_not_supported_query)
            },
            onClickRevokeOrder = { dexOrder, position ->
                PasswordInputDialog().setConfirmListener { password, dialog ->
                    dialog.dismiss()

                    (getViewModel() as DexOrderViewModel).revokeOrder(password, dexOrder) {
                        dexOrder.revokedFlag = true
                        getViewAdapter().notifyItemChanged(position)
                    }

                }.show(this@DexOrdersFragment.childFragmentManager)
            })
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        launch(Dispatchers.IO) {
            val result = initData(savedInstanceState)
            withContext(Dispatchers.Main) {
                if (result) {
                    initView()
                } else {
                    finishActivity()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        orderState?.let { outState.putString(EXTRA_KEY_ORDER_STATE, it) }
    }

    private fun initView() {
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

    private fun initData(savedInstanceState: Bundle?): Boolean {

        if (savedInstanceState != null) {
            orderState = savedInstanceState.getString(EXTRA_KEY_ORDER_STATE, null)
        } else if (arguments != null) {
            orderState = arguments!!.getString(EXTRA_KEY_ORDER_STATE, null)
        }

        return try {
            val currentAccount = AccountManager().currentAccount()
            accountAddress = currentAccount.address
            true
        } catch (e: Exception) {
            false
        }
    }
}