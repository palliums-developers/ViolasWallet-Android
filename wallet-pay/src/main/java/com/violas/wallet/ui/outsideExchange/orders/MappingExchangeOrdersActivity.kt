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
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.violas.wallet.common.EXTRA_KEY_WALLET_ADDRESS
import com.violas.wallet.common.EXTRA_KEY_WALLET_TYPE
import com.violas.wallet.repository.database.entity.AccountDO

/**
 * Created by elephant on 2020-02-18 12:09.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 映射兑换订单页
 */
class MappingExchangeOrdersActivity : BasePagingActivity<MappingExchangeOrderVO>() {

    companion object {
        fun start(context: Context, accountDO: AccountDO) {
            Intent(context, MappingExchangeOrdersActivity::class.java)
                .apply {
                    putExtra(EXTRA_KEY_WALLET_ADDRESS, accountDO.address)
                    putExtra(
                        EXTRA_KEY_WALLET_TYPE, when (accountDO.coinNumber) {
                            CoinTypes.Violas.coinType() -> 0
                            CoinTypes.Libra.coinType() -> 1
                            else -> 2
                        }
                    )
                }
                .start(context)
        }
    }

    private lateinit var walletAddress: String
    private var walletType: Int = -1

    private val viewModel by viewModels<MappingExchangeOrdersViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return modelClass.getConstructor(String::class.java, Int::class.java)
                    .newInstance(walletAddress, walletType)
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

        if (initData(savedInstanceState)) {
            initView()
        } else {
            close()
        }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        var address: String? = null
        var type: Int = -1
        if (savedInstanceState != null) {
            address = savedInstanceState.getString(EXTRA_KEY_WALLET_ADDRESS)
            type = savedInstanceState.getInt(EXTRA_KEY_WALLET_TYPE, type)
        } else if (intent != null) {
            address = intent.getStringExtra(EXTRA_KEY_WALLET_ADDRESS)
            type = intent.getIntExtra(EXTRA_KEY_WALLET_TYPE, type)
        }

        if (address.isNullOrEmpty() || type == -1) {
            return false
        }

        walletAddress = address
        walletType = type
        return true
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
        outState.putString(EXTRA_KEY_WALLET_ADDRESS, walletAddress)
        outState.putInt(EXTRA_KEY_WALLET_TYPE, walletType)
    }
}