package com.violas.wallet.ui.dexOrder

import android.os.Bundle
import androidx.lifecycle.Observer
import com.palliums.net.LoadState
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.getDrawableCompat
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingFragment
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.RevokeDexOrderEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.dex.DexOrderDTO
import com.violas.wallet.ui.dexOrder.details.DexOrderDetails2Activity
import com.violas.wallet.utils.authenticateAccount
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

class DexOrdersFragment : BasePagingFragment<DexOrderDTO>() {

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
    private val mAccountManager by lazy { AccountManager() }

    private val mViewModel by lazy {
        DexOrdersViewModel(
            accountAddress = currentAccount.address,
            //accountAddress = "0x07e92f79c67fdd6b80ed9103636a49511363de8c873bc709966fffb2e3fcd095",
            orderState = orderState
        )
    }

    private val mViewAdapter by lazy {
        DexOrdersViewAdapter(
            onClickItem = {
                //DexOrderDetailsActivity.start(requireContext(), it)
                DexOrderDetails2Activity.start(requireContext(), it)
            },
            onClickRevokeOrder = { dexOrder, position ->
                authenticateAccount(currentAccount, mAccountManager) {
                    mViewModel.revokeOrder(it, dexOrder) {
                        dexOrder.updateStateToRevoking()
                        getViewAdapter().notifyItemChanged(position)
                    }
                }
            })
    }

    override fun getViewModel(): PagingViewModel<DexOrderDTO> {
        return mViewModel
    }

    override fun getViewAdapter(): PagingViewAdapter<DexOrderDTO> {
        return mViewAdapter
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
        if (orderState.isNullOrEmpty()
            || orderState == DexOrderState.OPEN
            || orderState == DexOrderState.UNFINISHED
        ) {
            EventBus.getDefault().register(this)

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
            getString(
                if (orderState == DexOrderState.UNFINISHED)
                    R.string.tips_no_uncompleted_orders
                else
                    R.string.tips_no_completed_orders
            )
        )
        getDrawableCompat(R.mipmap.ic_no_transaction_record, requireContext())?.let {
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
            currentAccount = mAccountManager.currentAccount()

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

        mViewAdapter.currentList?.let {
            it.forEachIndexed { index, dexOrder ->
                if (dexOrder.id == event.orderId) {
                    dexOrder.updateStateToRevoking(event.updateDate)
                    mViewAdapter.notifyItemChanged(index)
                    return
                }
            }
        }
    }
}