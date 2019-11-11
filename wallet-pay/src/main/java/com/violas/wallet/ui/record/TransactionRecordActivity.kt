package com.violas.wallet.ui.record

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.paging.BasePagingActivity
import com.violas.wallet.base.paging.PagingViewAdapter
import com.violas.wallet.base.paging.PagingViewModel
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.EXTRA_KEY_ACCOUNT_ID
import com.violas.wallet.utils.start
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2019-11-06 17:15.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易记录页面
 */
class TransactionRecordActivity : BasePagingActivity<TransactionRecordVO>() {

    companion object {
        fun start(context: Context, accountId: Long) {
            Intent(context, TransactionRecordActivity::class.java)
                .apply {
                    putExtra(EXTRA_KEY_ACCOUNT_ID, accountId)
                }
                .start(context)
        }
    }

    private var mAccountId = -100L
    private lateinit var mAddress: String
    private lateinit var mCoinTypes: CoinTypes

    override fun initViewModel(): PagingViewModel<TransactionRecordVO> {
        return TransactionRecordViewModel(mAddress, mCoinTypes)
    }

    override fun initViewAdapter(): PagingViewAdapter<TransactionRecordVO> {
        return TransactionRecordViewAdapter { getViewModel().retry() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.transaction_record_title)

        launch(Dispatchers.IO) {
            val result = initData(savedInstanceState)
            withContext(Dispatchers.Main) {
                if (result) {
                    mPagingHandler.start()
                } else {
                    showToast(R.string.account_tips_not_found)
                    finish()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(EXTRA_KEY_ACCOUNT_ID, mAccountId)
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        if (savedInstanceState != null) {
            mAccountId = savedInstanceState.getLong(EXTRA_KEY_ACCOUNT_ID, mAccountId)
            if (mAccountId == -100L && intent != null) {
                mAccountId = intent.getLongExtra(EXTRA_KEY_ACCOUNT_ID, mAccountId)
            }
        } else if (intent != null) {
            mAccountId = intent.getLongExtra(EXTRA_KEY_ACCOUNT_ID, mAccountId)
        }

        if (mAccountId == -100L) {
            return false
        }

        return try {
            val accountDO = AccountManager().getAccountById(mAccountId)
            mCoinTypes = CoinTypes.parseCoinType(accountDO.coinNumber)
            mAddress = accountDO.address

            // code for test
            if (mCoinTypes == CoinTypes.VToken) {

            } else if (mCoinTypes == CoinTypes.Libra) {
                mAddress = "000000000000000000000000000000000000000000000000000000000a550c18"
            } else {
                mAddress = "15urYnyeJe3gwbGJ74wcX89Tz7ZtsFDVew"
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()

            false
        }
    }
}