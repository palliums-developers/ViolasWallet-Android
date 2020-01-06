package com.violas.wallet.ui.record

import android.os.Bundle
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.getDrawable
import com.palliums.utils.openBrowser
import com.palliums.widget.status.IStatusLayout
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

    private val mViewModel by lazy {
        TransactionRecordViewModel(mAccountAddress, mTokenAddress, mTokenName, mCoinTypes)
    }

    private val mViewAdapter by lazy {
        TransactionRecordViewAdapter(
            retryCallback = {
                mViewModel.retry()
            },
            onClickQuery = {
                if (it.url.isNullOrEmpty()) {
                    showToast(R.string.transaction_record_not_supported_query)
                } else {
                    if (!openBrowser(requireActivity(), it.url)) {
                        WebCommonActivity.start(requireActivity(), it.url)
                    }
                }
            })
    }

    override fun getViewModel(): PagingViewModel<TransactionRecordVO> {
        return mViewModel
    }

    override fun getViewAdapter(): PagingViewAdapter<TransactionRecordVO> {
        return mViewAdapter
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        if (initData()) {
            getStatusLayout()?.setTipsWithStatus(
                IStatusLayout.Status.STATUS_EMPTY,
                getString(R.string.tips_no_transaction_record)
            )
            getDrawable(R.mipmap.ic_no_transaction_record)?.let {
                getStatusLayout()?.setImageWithStatus(IStatusLayout.Status.STATUS_EMPTY, it)
            }

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