package com.violas.wallet.ui.transactionRecord

import android.os.Bundle
import android.view.View
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinType
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

    companion object {
        fun newInstance(
            walletAddress: String,
            coinNumber: Int,
            @TransactionType
            transactionType: Int,
            tokenId: String? = null,
            tokenDisplayName: String? = null
        ): TransactionRecordFragment {
            val args = Bundle().apply {
                putString(KEY_ONE, walletAddress)
                putInt(KEY_TWO, coinNumber)
                putInt(KEY_THREE, transactionType)
                tokenId?.let { putString(KEY_FOUR, it) }
                tokenDisplayName?.let { putString(KEY_FIVE, it) }
            }

            return TransactionRecordFragment().apply {
                arguments = args
            }
        }
    }

    private var mWalletAddress: String? = null
    private var mCoinNumber: Int = getViolasCoinType().coinNumber()
    @TransactionType
    private var mTransactionType: Int = TransactionType.ALL
    private var mTokenId: String? = null
    private var mTokenDisplayName: String? = null

    override fun lazyInitPagingViewModel(): PagingViewModel<TransactionRecordVO> {
        return TransactionRecordViewModel(
            mWalletAddress!!,
            mTokenId,
            mTokenDisplayName,
            mTransactionType,
            CoinType.parseCoinNumber(mCoinNumber)
        )
    }

    override fun lazyInitPagingViewAdapter(): PagingViewAdapter<TransactionRecordVO> {
        return TransactionRecordViewAdapter {
            TransactionDetailsActivity.start(requireContext(), it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!initData(savedInstanceState)) {
            finishActivity()
        }
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)

        getPagingHandler().init()
        getStatusLayout()?.setTipsWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            getString(R.string.txn_records_desc_records_empty)
        )
        getPagingHandler().start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mWalletAddress?.let { outState.putString(KEY_ONE, it) }
        outState.putInt(KEY_TWO, mCoinNumber)
        outState.putInt(KEY_THREE, mTransactionType)
        mTokenId?.let { outState.putString(KEY_FOUR, it) }
        mTokenDisplayName?.let { outState.putString(KEY_FIVE, it) }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        try {
            val bundle = savedInstanceState ?: arguments ?: return false

            mWalletAddress = bundle.getString(KEY_ONE, null) ?: return false
            mCoinNumber = bundle.getInt(KEY_TWO, mCoinNumber)
            mTransactionType = bundle.getInt(KEY_THREE, mTransactionType)
            mTokenId = bundle.getString(KEY_FOUR)
            mTokenDisplayName = bundle.getString(KEY_FIVE)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}