package com.violas.wallet.ui.record

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.common.KEY_THREE
import com.violas.wallet.common.KEY_TWO
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
            tokenIdx: Long? = null,
            tokenName: String? = null
        ) {
            Intent(context, TransactionRecordActivity::class.java)
                .apply {
                    putExtra(KEY_ONE, accountId)
                    tokenIdx?.let { putExtra(KEY_TWO, it) }
                    tokenName?.let { putExtra(KEY_THREE, it) }
                }
                .start(context)
        }
    }

    private var mAccountId = -100L
    private var mTokenIdx: Long? = null
    private var mTokenName: String? = null
    private lateinit var mWalletAddress: String
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
                            mWalletAddress,
                            mCoinTypes,
                            mTokenIdx,
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
        outState.putLong(KEY_ONE, mAccountId)
        mTokenIdx?.let { outState.putLong(KEY_TWO, it) }
        mTokenName?.let { outState.putString(KEY_THREE, it) }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        if (savedInstanceState != null) {
            mAccountId = savedInstanceState.getLong(KEY_ONE, -100)
            if (savedInstanceState.containsKey(KEY_TWO)) {
                mTokenIdx = savedInstanceState.getLong(KEY_TWO)
            }
            if (savedInstanceState.containsKey(KEY_THREE)) {
                mTokenName = savedInstanceState.getString(KEY_THREE)
            }
        } else if (intent != null) {
            mAccountId = intent.getLongExtra(KEY_TWO, -100)
            if (intent.hasExtra(KEY_TWO)) {
                mTokenIdx = intent.getLongExtra(KEY_TWO, 0)
            }
            if (intent.hasExtra(KEY_THREE)) {
                mTokenName = intent.getStringExtra(KEY_THREE)
            }
        }

        if (mAccountId == -100L) {
            return false
        }

        return try {
            val accountDO = AccountManager().getAccountById(mAccountId)
            mCoinTypes = CoinTypes.parseCoinType(accountDO.coinNumber)
            mWalletAddress = accountDO.address

            // code for test, test address
            /*if (BuildConfig.DEBUG) {
                when (mCoinTypes) {
                    CoinTypes.Bitcoin -> {
                        mAddress = "3MzUcaPHN2sTGQMkuW2sSfrWTbLHTeofnz"
                    }
                    CoinTypes.BitcoinTest -> {
                        mAddress = "n1QvA4WTJfosuKoWeHE5Xdggf5j9P5gBtg"
                    }
                    CoinTypes.Libra -> {
                        mAddress =
                            "000000000000000000000000000000000000000000000000000000000a550c18"
                    }
                }
            }*/

            true
        } catch (e: Exception) {
            e.printStackTrace()

            false
        }
    }
}