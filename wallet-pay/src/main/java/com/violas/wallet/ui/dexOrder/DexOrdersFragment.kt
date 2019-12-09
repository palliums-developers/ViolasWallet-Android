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
        private const val EXTRA_KEY_BASE_TOKEN_ADDRESS = "EXTRA_KEY_BASE_TOKEN_ADDRESS"
        private const val EXTRA_KEY_QUOTE_TOKEN_ADDRESS = "EXTRA_KEY_QUOTE_TOKEN_ADDRESS"

        fun newInstance(
            @DexOrdersState
            orderState: String?,
            baseTokenAddress: String? = null,
            quoteTokenAddress: String? = null
        ): DexOrdersFragment {
            val bundle = Bundle().apply {
                orderState?.let { putString(EXTRA_KEY_ORDER_STATE, it) }
                baseTokenAddress?.let { putString(EXTRA_KEY_BASE_TOKEN_ADDRESS, it) }
                quoteTokenAddress?.let { putString(EXTRA_KEY_QUOTE_TOKEN_ADDRESS, it) }
            }

            return DexOrdersFragment().apply {
                arguments = bundle
            }
        }
    }

    @DexOrdersState
    private var orderState: String? = null
    private var baseTokenAddress: String? = null
    private var quoteTokenAddress: String? = null
    private lateinit var accountAddress: String

    override fun initViewModel(): PagingViewModel<DexOrderDTO> {
        return DexOrdersViewModel(accountAddress, orderState, baseTokenAddress, quoteTokenAddress)
    }

    private fun showItemAllInfo(): Boolean {
        return baseTokenAddress.isNullOrEmpty() || quoteTokenAddress.isNullOrEmpty()
    }

    override fun initViewAdapter(): PagingViewAdapter<DexOrderDTO> {
        return DexOrdersViewAdapter(
            viewModel = getViewModel() as DexOrdersViewModel,
            showItemAllInfo = showItemAllInfo(),
            retryCallback = { getViewModel().retry() },
            onOpenOrderDetails = {
                DexOrderDetailsActivity.start(requireContext(), it)
            },
            onOpenBrowserView = {
                // TODO violas浏览器暂未实现
                showToast(R.string.transaction_record_not_supported_query)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        orderState?.let { outState.putString(EXTRA_KEY_ORDER_STATE, it) }
        baseTokenAddress?.let { outState.putString(EXTRA_KEY_BASE_TOKEN_ADDRESS, it) }
        quoteTokenAddress?.let { outState.putString(EXTRA_KEY_QUOTE_TOKEN_ADDRESS, it) }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {

        if (savedInstanceState != null) {
            orderState = savedInstanceState.getString(EXTRA_KEY_ORDER_STATE, null)
            baseTokenAddress = savedInstanceState.getString(EXTRA_KEY_BASE_TOKEN_ADDRESS, null)
            quoteTokenAddress = savedInstanceState.getString(EXTRA_KEY_QUOTE_TOKEN_ADDRESS, null)
        } else if (arguments != null) {
            orderState = arguments!!.getString(EXTRA_KEY_ORDER_STATE, orderState)
            baseTokenAddress = arguments!!.getString(EXTRA_KEY_BASE_TOKEN_ADDRESS, null)
            quoteTokenAddress = arguments!!.getString(EXTRA_KEY_QUOTE_TOKEN_ADDRESS, null)
        }

        return try {
            val currentAccount = AccountManager().currentAccount()
            accountAddress = currentAccount.address
            accountAddress = "0xe744bc4894feef25111dc40ec39644468e797ec07270c3c6d234675630c1797f"
            true
        } catch (e: Exception) {
            false
        }
    }
}