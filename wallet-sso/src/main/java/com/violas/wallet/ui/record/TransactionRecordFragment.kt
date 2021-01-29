package com.violas.wallet.ui.record

import android.os.Bundle
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.getDrawableCompat
import com.palliums.utils.openBrowser
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingFragment
import com.violas.wallet.common.KEY_FOUR
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.common.KEY_THREE
import com.violas.wallet.common.KEY_TWO
import com.violas.wallet.ui.web.WebCommonActivity

/**
 * Created by elephant on 2019-12-16 15:14.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class TransactionRecordFragment : BasePagingFragment<TransactionRecordVO>() {

    private lateinit var mAccountAddress: String
    private lateinit var mCoinTypes: CoinTypes
    private var mTokenIdx: Long? = null
    private var mTokenName: String? = null

    companion object {
        fun newInstance(
            accountAddress: String,
            coinTypes: CoinTypes,
            tokenIdx: Long? = null,
            tokenName: String? = null
        ): TransactionRecordFragment {
            val args = Bundle().apply {
                putString(KEY_ONE, accountAddress)
                putSerializable(KEY_TWO, coinTypes)
                tokenIdx?.let { putLong(KEY_THREE, it) }
                tokenName?.let { putString(KEY_FOUR, it) }
            }

            return TransactionRecordFragment().apply {
                arguments = args
            }
        }
    }

    private val mViewModel by lazy {
        TransactionRecordViewModel(mAccountAddress, mTokenIdx, mTokenName, mCoinTypes)
    }

    private val mViewAdapter by lazy {
        TransactionRecordViewAdapter {
            if (it.url.isNullOrEmpty()) {
                showToast(R.string.transaction_record_not_supported_query)
            } else {
                if (!openBrowser(requireActivity(), it.url)) {
                    WebCommonActivity.start(requireActivity(), it.url)
                }
            }
        }
    }

    override fun getPagingViewModel(): PagingViewModel<TransactionRecordVO> {
        return mViewModel
    }

    override fun getPagingViewAdapter(): PagingViewAdapter<TransactionRecordVO> {
        return mViewAdapter
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        if (initData()) {
            mPagingHandler.init()
            getStatusLayout()?.setTipsWithStatus(
                IStatusLayout.Status.STATUS_EMPTY,
                getString(R.string.tips_no_transaction_record)
            )
            getDrawableCompat(R.mipmap.ic_no_transaction_record, requireContext())?.let {
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

            mAccountAddress = arguments!!.getString(KEY_ONE, null) ?: return false
            mCoinTypes = arguments!!.getSerializable(KEY_TWO) as CoinTypes
            if (arguments!!.containsKey(KEY_THREE)) {
                mTokenIdx = arguments!!.getLong(KEY_THREE)
            }
            if (arguments!!.containsKey(KEY_FOUR)) {
                mTokenName = arguments!!.getString(KEY_FOUR)
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