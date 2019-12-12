package com.violas.wallet.ui.dexOrder

import android.os.Bundle
import androidx.annotation.StringDef
import androidx.lifecycle.Observer
import com.palliums.net.LoadState
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.violas.wallet.base.BasePagingFragment
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.RevokeDexOrderEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.widget.dialog.PasswordInputDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

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
    private lateinit var currentAccount: AccountDO

    override fun initViewModel(): PagingViewModel<DexOrderVO> {
        return DexOrderViewModel(
            //accountAddress = currentAccount.address,
            accountAddress = "0x07e92f79c67fdd6b80ed9103636a49511363de8c873bc709966fffb2e3fcd095",
            orderState = orderState,
            giveTokenAddress = null,
            getTokenAddress = null
        )
    }

    override fun initViewAdapter(): PagingViewAdapter<DexOrderVO> {
        return DexOrderViewAdapter(
            retryCallback = { getViewModel().retry() },
            onOpenOrderDetails = {
                //DexOrderDetailsActivity.start(requireContext(), it)
                DexOrderDetails2Activity.start(requireContext(), it)
            },
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
                        }

                    ) {
                        dialog.dismiss()
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

    private fun initView() {
        if (orderState.isNullOrEmpty() || orderState == DexOrdersState.OPEN) {
            EventBus.getDefault().register(this)
        }

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
            currentAccount = AccountManager().currentAccount()

            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        orderState?.let { outState.putString(EXTRA_KEY_ORDER_STATE, it) }
    }

    override fun onDestroyView() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onDestroyView()
    }

    @Subscribe
    fun onRevokeDexOrderEvent(event: RevokeDexOrderEvent) {
        if (event.orderId.isEmpty()) {
            return
        }

        getViewAdapter().currentList?.let {
            it.forEachIndexed { index, dexOrderVO ->
                if (dexOrderVO.dexOrderDTO.id == event.orderId) {
                    dexOrderVO.revokedFlag = true
                    dexOrderVO.dexOrderDTO.date = System.currentTimeMillis()

                    getViewAdapter().notifyItemChanged(index)
                }
            }
        }
    }
}