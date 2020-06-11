package com.violas.wallet.ui.transactionRecord

import android.os.Bundle
import android.view.View
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.getDrawable
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinTypes
import com.quincysx.crypto.bip44.CoinType
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingFragment
import com.violas.wallet.common.*
import com.violas.wallet.ui.transactionDetails.TransactionDetailsActivity

/**
 * Created by elephant on 2019-12-16 15:14.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易记录视图
 */
class TransactionRecordFragment : BasePagingFragment<TransactionRecordVO>() {

    private var mWalletAddress: String? = null
    private var mCoinNumber: Int = CoinTypes.Violas.coinType()
    @TransactionType
    private var mTransactionType: Int = TransactionType.ALL
    private var mTokenAddress: String? = null
    private var mTokenName: String? = null

    private var lazyInitTag = false

    companion object {
        fun newInstance(
            walletAddress: String,
            coinNumber: Int,
            @TransactionType
            transactionType: Int,
            tokenAddress: String? = null,
            tokenName: String? = null
        ): TransactionRecordFragment {
            val args = Bundle().apply {
                putString(KEY_ONE, walletAddress)
                putInt(KEY_TWO, coinNumber)
                putInt(KEY_THREE, transactionType)
                tokenAddress?.let { putString(KEY_FOUR, it) }
                tokenName?.let { putString(KEY_FIVE, it) }
            }

            return TransactionRecordFragment().apply {
                arguments = args
            }
        }
    }

    private val mViewModel by lazy {
        TransactionRecordViewModel(
            mWalletAddress!!,
            mTokenAddress,
            mTokenName,
            mTransactionType,
            CoinTypes.parseCoinType(mCoinNumber)
        )
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

        if (!initData(savedInstanceState)) {
            finishActivity()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!lazyInitTag) {
            lazyInitTag = true
            onLazy2InitView()
        }
    }

    private fun onLazy2InitView() {
        getStatusLayout()?.setTipsWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            getString(R.string.tips_no_transaction_record)
        )
        getDrawable(R.mipmap.ic_no_transaction_record)?.let {
            getStatusLayout()?.setImageWithStatus(IStatusLayout.Status.STATUS_EMPTY, it)
        }

        mPagingHandler.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mWalletAddress?.let { outState.putString(KEY_ONE, it) }
        outState.putInt(KEY_TWO, mCoinNumber)
        outState.putInt(KEY_THREE, mTransactionType)
        mTokenAddress?.let { outState.putString(KEY_FOUR, it) }
        mTokenName?.let { outState.putString(KEY_FIVE, it) }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        try {
            val bundle = savedInstanceState ?: arguments ?: return false

            mWalletAddress = bundle.getString(KEY_ONE, null) ?: return false
            mCoinNumber = bundle.getInt(KEY_TWO, mCoinNumber)
            mTransactionType = bundle.getInt(KEY_THREE, mTransactionType)
            mTokenAddress = bundle.getString(KEY_FOUR)
            mTokenName = bundle.getString(KEY_FIVE)

            // code for test
            mWalletAddress =
                if (mCoinNumber == CoinTypes.Violas.coinType()
                    || mCoinNumber == CoinTypes.Libra.coinType()
                ) {
                    "f4174e9eabcb2e968e22da4c75ac653b"
                } else {
                    "2NGZrVvZG92qGYqzTLjCAewvPZ7JE8S8VxE"
                }

            return true
        } catch (e: Exception) {
            return false
        }
    }
}