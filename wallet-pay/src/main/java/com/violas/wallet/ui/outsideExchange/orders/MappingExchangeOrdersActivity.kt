package com.violas.wallet.ui.outsideExchange.orders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.start
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.violas.wallet.common.EXTRA_KEY_ACCOUNT_ADDRESS

/**
 * Created by elephant on 2020-02-18 12:09.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 映射兑换订单页
 */
class MappingExchangeOrdersActivity : BasePagingActivity<MappingExchangeOrderVO>() {

    companion object {
        fun start(context: Context, accountAddress: String) {
            Intent(context, MappingExchangeOrdersActivity::class.java)
                .apply {
                    putExtra(EXTRA_KEY_ACCOUNT_ADDRESS, accountAddress)
                }
                .start(context)
        }
    }

    private lateinit var accountAddress: String

    private val viewModel by viewModels<MappingExchangeOrdersViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return MappingExchangeOrdersViewModel(accountAddress) as T
            }
        }
    }

    private val viewAdapter by lazy {
        MappingExchangeOrdersViewAdapter { viewModel.retry() }
    }

    override fun getViewModel(): PagingViewModel<MappingExchangeOrderVO> {
        return viewModel
    }

    override fun getViewAdapter(): PagingViewAdapter<MappingExchangeOrderVO> {
        return viewAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var address: String? = null
        if (savedInstanceState != null) {
            address = savedInstanceState.getString(EXTRA_KEY_ACCOUNT_ADDRESS)
        } else if (intent != null) {
            address = intent.getStringExtra(EXTRA_KEY_ACCOUNT_ADDRESS)
        }
        if (address.isNullOrEmpty()) {
            close()
            return
        }

        accountAddress = address

        initView()
    }

    private fun initView() {
        setTitle(R.string.title_mapping_exchange_orders)

        getStatusLayout()?.setTipsWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            getString(R.string.tips_no_mapping_exchange_orders)
        )
        getDrawable(R.mipmap.ic_no_transaction_record)?.let {
            getStatusLayout()?.setImageWithStatus(IStatusLayout.Status.STATUS_EMPTY, it)
        }

        mPagingHandler.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_KEY_ACCOUNT_ADDRESS, accountAddress)
    }
}