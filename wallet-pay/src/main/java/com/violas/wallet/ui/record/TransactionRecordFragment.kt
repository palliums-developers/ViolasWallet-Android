package com.violas.wallet.ui.record

import android.os.Bundle
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingFragment
import com.violas.wallet.common.EXTRA_KEY_ACCOUNT_ADDRESS
import com.violas.wallet.common.EXTRA_KEY_COIN_TYPES
import com.violas.wallet.common.EXTRA_KEY_TOKEN_ADDRESS
import com.violas.wallet.common.EXTRA_KEY_TOKEN_NAME
import com.violas.wallet.ui.web.WebCommonActivity

/**
 * Created by elephant on 2019-12-16 15:14.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class TransactionRecordFragment : BasePagingFragment<TransactionRecordVO>() {

    private var mTokenAddress: String? = null
    private var mTokenName: String? = null
    private lateinit var mAccountAddress: String
    private lateinit var mCoinTypes: CoinTypes

    companion object {
        fun newInstance(
            accountAddress: String,
            coinTypes: CoinTypes,
            tokenAddress: String? = null,
            tokenName: String? = null
        ): TransactionRecordFragment {
            val args = Bundle().apply {
                putString(EXTRA_KEY_ACCOUNT_ADDRESS, accountAddress)
                putSerializable(EXTRA_KEY_COIN_TYPES, coinTypes)
                tokenAddress?.let { putString(EXTRA_KEY_TOKEN_ADDRESS, tokenAddress) }
                tokenAddress?.let { putString(EXTRA_KEY_TOKEN_NAME, tokenName) }
            }

            return TransactionRecordFragment().apply {
                arguments = args
            }
        }
    }

    override fun initViewModel(): PagingViewModel<TransactionRecordVO> {
        return TransactionRecordViewModel(mAccountAddress, mTokenAddress, mTokenName, mCoinTypes)
    }

    override fun initViewAdapter(): PagingViewAdapter<TransactionRecordVO> {
        return TransactionRecordViewAdapter(
            retryCallback = {
                getViewModel().retry()
            },
            onClickQuery = {
                if (it.url.isNullOrEmpty()) {
                    showToast(R.string.transaction_record_not_supported_query)
                } else {
                    WebCommonActivity.start(requireActivity(), it.url)
                }
            })
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        if (initData()) {
            mPagingHandler.start()
        } else {
            finishActivity()
        }
    }

    private fun initData(): Boolean {
        try {
            if (arguments == null) {
                return false
            }

            mAccountAddress = arguments!!.getString(EXTRA_KEY_ACCOUNT_ADDRESS, null)
                ?: return false
            mCoinTypes = arguments!!.getSerializable(EXTRA_KEY_COIN_TYPES) as CoinTypes
            mTokenAddress = arguments!!.getString(EXTRA_KEY_TOKEN_ADDRESS, null)
            mTokenName = arguments!!.getString(EXTRA_KEY_TOKEN_NAME, null)

            // code for test
            /*if (mCoinTypes == CoinTypes.Violas) {

                } else if (mCoinTypes == CoinTypes.Libra) {
                    mAddress = "000000000000000000000000000000000000000000000000000000000a550c18"
                } else {
                    mAddress = "15urYnyeJe3gwbGJ74wcX89Tz7ZtsFDVew"
                }*/

            return true
        } catch (e: Exception) {
            return false
        }
    }
}