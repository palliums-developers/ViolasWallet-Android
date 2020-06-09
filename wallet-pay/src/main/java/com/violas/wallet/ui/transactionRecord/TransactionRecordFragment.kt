package com.violas.wallet.ui.transactionRecord

import android.os.Bundle
import android.view.View
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.getDrawable
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingFragment
import com.violas.wallet.common.KEY_FOUR
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.common.KEY_THREE
import com.violas.wallet.common.KEY_TWO
import com.violas.wallet.ui.transactionDetails.TransactionDetailsActivity

/**
 * Created by elephant on 2019-12-16 15:14.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易记录视图
 */
class TransactionRecordFragment : BasePagingFragment<TransactionRecordVO>() {

    private var mWalletAddress: String? = null
    private var mCoinTypes: CoinTypes? = null
    @TransactionType
    private var mTransactionType: Int = TransactionType.ALL
    private var mTokenAddress: String? = null

    private var savedInstanceState: Bundle? = null
    private var lazyInitTag = false

    companion object {
        fun newInstance(
            accountAddress: String,
            coinTypes: CoinTypes,
            @TransactionType
            transactionType: Int,
            tokenAddress: String? = null
        ): TransactionRecordFragment {
            val args = Bundle().apply {
                putString(KEY_ONE, accountAddress)
                putSerializable(KEY_TWO, coinTypes)
                putInt(KEY_THREE, transactionType)
                tokenAddress?.let { putString(KEY_FOUR, it) }
            }

            return TransactionRecordFragment().apply {
                arguments = args
            }
        }
    }

    private val mViewModel by lazy {
        TransactionRecordViewModel(mWalletAddress!!, mTokenAddress, mTransactionType, mCoinTypes!!)
    }

    private val mViewAdapter by lazy {
        TransactionRecordViewAdapter(
            retryCallback = {
                mViewModel.retry()
            },
            onItemClick = {
                TransactionDetailsActivity.start(requireContext(), it)
            })
    }

    override fun getViewModel(): PagingViewModel<TransactionRecordVO> {
        return mViewModel
    }

    override fun getViewAdapter(): PagingViewAdapter<TransactionRecordVO> {
        return mViewAdapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.savedInstanceState = savedInstanceState
    }

    override fun onResume() {
        super.onResume()
        if (!lazyInitTag) {
            lazyInitTag = true
            onLazy2InitView(savedInstanceState)
        }
    }

    private fun onLazy2InitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)
        if (initData(savedInstanceState)) {
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mWalletAddress?.let { outState.putString(KEY_ONE, it) }
        mCoinTypes?.let { outState.putSerializable(KEY_TWO, it) }
        outState.putInt(KEY_THREE, mTransactionType)
        mTokenAddress?.let { outState.putString(KEY_FOUR, it) }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        try {
            val bundle = savedInstanceState ?: arguments ?: return false

            mWalletAddress = bundle.getString(KEY_ONE, null) ?: return false
            mCoinTypes = bundle.getSerializable(KEY_TWO) as CoinTypes
            mTransactionType = bundle.getInt(KEY_THREE, TransactionType.ALL)
            if (bundle.containsKey(KEY_FOUR)) {
                mTokenAddress = bundle.getString(KEY_FOUR)
            }

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