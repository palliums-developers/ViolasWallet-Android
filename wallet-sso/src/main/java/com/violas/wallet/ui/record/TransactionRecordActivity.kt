package com.violas.wallet.ui.record

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.EXTRA_KEY_ACCOUNT_ID
import com.violas.wallet.common.EXTRA_KEY_TOKEN_ADDRESS
import com.violas.wallet.common.EXTRA_KEY_TOKEN_NAME
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
            tokenAddress: String? = null,
            tokenName: String? = null
        ) {
            Intent(context, TransactionRecordActivity::class.java)
                .apply {
                    putExtra(EXTRA_KEY_ACCOUNT_ID, accountId)
                    tokenAddress?.let { putExtra(EXTRA_KEY_TOKEN_ADDRESS, it) }
                    tokenName?.let { putExtra(EXTRA_KEY_TOKEN_NAME, it) }
                }
                .start(context)
        }
    }

    private var mAccountId = -100L
    private var mTokenAddress: String? = null
    private var mTokenName: String? = null
    private lateinit var mAddress: String
    private lateinit var mCoinTypes: CoinTypes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.transaction_record_title)

        findFragment(TransactionRecordFragment::class.java)?.pop()

        launch(Dispatchers.IO) {
            val result = initData(savedInstanceState)
            withContext(Dispatchers.Main) {
                if (result) {
                    loadRootFragment(
                        R.id.flFragmentContainer,
                        TransactionRecordFragment.newInstance(
                            mAddress,
                            mCoinTypes,
                            mTokenAddress,
                            mTokenName
                        )
                    )
                } else {
                    showToast(R.string.account_tips_not_found)
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
        outState.putLong(EXTRA_KEY_ACCOUNT_ID, mAccountId)
        mTokenAddress?.let { outState.putString(EXTRA_KEY_TOKEN_ADDRESS, it) }
        mTokenName?.let { outState.putString(EXTRA_KEY_TOKEN_NAME, it) }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        if (savedInstanceState != null) {
            mAccountId = savedInstanceState.getLong(EXTRA_KEY_ACCOUNT_ID, mAccountId)
            mTokenAddress = savedInstanceState.getString(EXTRA_KEY_TOKEN_ADDRESS, null)
            mTokenName = savedInstanceState.getString(EXTRA_KEY_TOKEN_NAME, null)
        } else if (intent != null) {
            mAccountId = intent.getLongExtra(EXTRA_KEY_ACCOUNT_ID, mAccountId)
            mTokenAddress = intent.getStringExtra(EXTRA_KEY_TOKEN_ADDRESS)
            mTokenName = intent.getStringExtra(EXTRA_KEY_TOKEN_NAME)
        }

        if (mAccountId == -100L) {
            return false
        }

        return try {
            val accountDO = AccountManager().getAccountById(mAccountId)
            mCoinTypes = CoinTypes.parseCoinType(accountDO.coinNumber)
            mAddress = accountDO.address

            // code for test
            /*if (mCoinTypes == CoinTypes.Violas) {

            } else if (mCoinTypes == CoinTypes.Libra) {
                mAddress = "000000000000000000000000000000000000000000000000000000000a550c18"
            } else {
                mAddress = "15urYnyeJe3gwbGJ74wcX89Tz7ZtsFDVew"
            }*/

            true
        } catch (e: Exception) {
            e.printStackTrace()

            false
        }
    }
}