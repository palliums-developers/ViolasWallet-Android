package com.violas.wallet.ui.dexOrder

import android.os.Bundle
import androidx.lifecycle.Observer
import com.palliums.net.LoadState
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.getDrawable
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingFragment
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.RevokeDexOrderEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.dexOrder.details.DexOrderDetails2Activity
import com.violas.wallet.widget.dialog.PasswordInputDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Created by elephant on 2019-12-06 12:02.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易中心订单
 */

class DexOrdersFragment : BasePagingFragment<DexOrderVO>() {

    companion object {
        private const val EXTRA_KEY_ORDER_STATE = "EXTRA_KEY_ORDER_STATE"

        /**
         * @param orderState 订单状态，若为null则查询所有
         */
        fun newInstance(
            @DexOrderState
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

    @DexOrderState
    private var orderState: String? = null
    private lateinit var currentAccount: AccountDO

    override fun initViewModel(): PagingViewModel<DexOrderVO> {
        return DexOrdersViewModel(
            accountAddress = currentAccount.address,
            //accountAddress = "0x07e92f79c67fdd6b80ed9103636a49511363de8c873bc709966fffb2e3fcd095",
            orderState = orderState,
            giveTokenAddress = null,
            getTokenAddress = null
        )
    }

    override fun initViewAdapter(): PagingViewAdapter<DexOrderVO> {
        return DexOrdersViewAdapter(
            retryCallback = { getViewModel().retry() },
            onOpenOrderDetails = {
                //DexOrderDetailsActivity.start(requireContext(), it)
                DexOrderDetails2Activity.start(requireContext(), it)
            },
            onClickRevokeOrder = { dexOrder, position ->

                PasswordInputDialog().setConfirmListener { password, dialog ->

                    if (!(getViewModel() as DexOrdersViewModel).revokeOrder(
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
                            dexOrder.dto.date = System.currentTimeMillis()

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
        if (orderState.isNullOrEmpty() || orderState == DexOrderState.OPEN) {
            EventBus.getDefault().register(this)

            (getViewModel() as DexOrdersViewModel).loadState.observe(this, Observer {
                when (it.status) {
                    LoadState.Status.RUNNING -> {
                        showProgress()
                    }

                    else -> {
                        dismissProgress()
                    }
                }
            })

            (getViewModel() as DexOrdersViewModel).tipsMessage.observe(this, Observer {
                if (it.isNotEmpty()) {
                    showToast(it)
                }
            })
        }

        getStatusLayout()?.setTipsWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            getString(
                if (orderState == DexOrderState.OPEN)
                    R.string.tips_no_uncompleted_orders
                else
                    R.string.tips_no_completed_orders
            )
        )
        getDrawable(R.mipmap.ic_no_transaction_record)?.let {
            getStatusLayout()?.setImageWithStatus(IStatusLayout.Status.STATUS_EMPTY, it)
        }

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRevokeDexOrderEvent(event: RevokeDexOrderEvent) {
        if (event.orderId.isEmpty()) {
            return
        }

        getViewAdapter().currentList?.let {
            it.forEachIndexed { index, dexOrderVO ->
                if (dexOrderVO.dto.id == event.orderId) {
                    dexOrderVO.revokedFlag = true
                    dexOrderVO.dto.date = System.currentTimeMillis()

                    getViewAdapter().notifyItemChanged(index)
                }
            }
        }
    }
}