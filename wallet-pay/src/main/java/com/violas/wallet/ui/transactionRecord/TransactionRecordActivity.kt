package com.violas.wallet.ui.transactionRecord

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.start
import com.quincysx.crypto.CoinType
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2019-11-06 17:15.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易记录页面
 */
class TransactionRecordActivity : BaseAppActivity() {

    companion object {
        fun start(
            context: Context,
            accountId: Long,
            tokenId: String? = null
        ) {
            Intent(context, TransactionRecordActivity::class.java)
                .apply {
                    putExtra(KEY_ONE, accountId)
                    tokenId?.let { putExtra(KEY_TWO, it) }
                }
                .start(context)
        }
    }

    private lateinit var mWalletAddress: String
    private lateinit var mCoinType: CoinType

    private var mAccountId = -100L
    private var mTokenId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.txn_records_title)

        findFragment(TransactionRecordFragment::class.java)?.pop()

        launch(Dispatchers.IO) {
            val result = initData(savedInstanceState)
            withContext(Dispatchers.Main) {
                if (result) {
                    loadRootFragment(
                        R.id.flFragmentContainer,
                        TransactionRecordFragment.newInstance(
                            mWalletAddress,
                            mCoinType.coinNumber(),
                            TransactionType.ALL,
                            mTokenId
                        )
                    )
                } else {
                    showToast(R.string.common_tips_account_error)
                    finish()
                }
            }
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_transaction_record
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_ONE, mAccountId)
        mTokenId?.let { outState.putString(KEY_TWO, it) }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        if (savedInstanceState != null) {
            mAccountId = savedInstanceState.getLong(KEY_ONE, -100)
            if (savedInstanceState.containsKey(KEY_TWO)) {
                mTokenId = savedInstanceState.getString(KEY_TWO)
            }
        } else if (intent != null) {
            mAccountId = intent.getLongExtra(KEY_ONE, -100)
            if (intent.hasExtra(KEY_TWO)) {
                mTokenId = intent.getStringExtra(KEY_TWO)
            }
        }

        if (mAccountId == -100L) {
            return false
        }

        return try {
            val accountDO = AccountManager().getAccountById(mAccountId)
            mCoinType = CoinType.parseCoinNumber(accountDO.coinNumber)
            mWalletAddress = accountDO.address
            true
        } catch (e: Exception) {
            e.printStackTrace()

            false
        }
    }
}