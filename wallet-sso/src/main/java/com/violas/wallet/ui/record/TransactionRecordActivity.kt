package com.violas.wallet.ui.record

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingActivity
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.EXTRA_KEY_ACCOUNT_ID
import com.violas.wallet.common.EXTRA_KEY_TOKEN
import com.violas.wallet.repository.database.entity.TokenDo
import com.violas.wallet.ui.web.WebCommonActivity
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
        fun start(context: Context, accountId: Long, tokenDO: TokenDo? = null) {
            Intent(context, TransactionRecordActivity::class.java)
                .apply {
                    putExtra(EXTRA_KEY_ACCOUNT_ID, accountId)
                    if (tokenDO != null) {
                        putExtra(EXTRA_KEY_TOKEN, tokenDO)
                    }
                }
                .start(context)
        }
    }

    private var mAccountId = -100L
    private var mTokenDO: TokenDo? = null
    private lateinit var mAddress: String
    private lateinit var mCoinTypes: CoinTypes

    override fun initViewModel(): PagingViewModel<TransactionRecordVO> {
        return TransactionRecordViewModel(mAddress, mTokenDO, mCoinTypes)
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
                    WebCommonActivity.start(this, it.url)
                }
            })
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
        if (mTokenDO != null) {
            outState.putParcelable(EXTRA_KEY_TOKEN, mTokenDO)
        }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        if (savedInstanceState != null) {
            mAccountId = savedInstanceState.getLong(EXTRA_KEY_ACCOUNT_ID, mAccountId)
            mTokenDO = savedInstanceState.getParcelable(EXTRA_KEY_TOKEN)
            if (mAccountId == -100L && intent != null) {
                mAccountId = intent.getLongExtra(EXTRA_KEY_ACCOUNT_ID, mAccountId)
            }
            if (mTokenDO == null && intent != null) {
                mTokenDO = intent.getParcelableExtra(EXTRA_KEY_TOKEN)
            }
        } else if (intent != null) {
            mAccountId = intent.getLongExtra(EXTRA_KEY_ACCOUNT_ID, mAccountId)
            mTokenDO = intent.getParcelableExtra(EXTRA_KEY_TOKEN)
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