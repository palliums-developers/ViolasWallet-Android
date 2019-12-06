package com.violas.wallet.ui.dexOrder

import android.os.Bundle
import androidx.annotation.StringDef
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingFragment
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.http.dex.DexOrderDTO
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

class DexOrdersFragment : BasePagingFragment<DexOrderDTO>() {

    companion object {
        private const val EXTRA_KEY_ORDER_STATE = "EXTRA_KEY_ORDER_STATE"

        fun newInstance(@DexOrdersState orderState: String): DexOrdersFragment {
            val bundle = Bundle().apply {
                putString(EXTRA_KEY_ORDER_STATE, orderState)
            }

            return DexOrdersFragment().apply {
                arguments = bundle
            }
        }
    }

    private var orderState = DexOrdersState.OPEN
    private lateinit var address: String

    override fun initViewModel(): PagingViewModel<DexOrderDTO> {
        return DexOrdersViewModel(address, orderState)
    }

    override fun initViewAdapter(): PagingViewAdapter<DexOrderDTO> {
        return DexOrdersViewAdapter(
            viewModel = getViewModel() as DexOrdersViewModel,
            retryCallback = {
                getViewModel().retry()
            },
            onClickItem = {

            })
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        launch(Dispatchers.IO) {
            val result = initData(savedInstanceState)
            withContext(Dispatchers.Main) {
                if (result) {
                    mPagingHandler.start()
                } else {
                    finishActivity()
                }
            }
        }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {

        if (savedInstanceState != null) {
            orderState = savedInstanceState.getString(EXTRA_KEY_ORDER_STATE, orderState)
        } else if (arguments != null) {
            orderState = arguments!!.getString(EXTRA_KEY_ORDER_STATE, orderState)
        }

        return try {
            val currentAccount = AccountManager().currentAccount()
            address = currentAccount.address
            true
        } catch (e: Exception) {
            false
        }
    }
}