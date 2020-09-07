package com.violas.wallet.ui.bank.details

import android.os.Bundle
import android.view.View
import com.violas.wallet.base.BasePagingFragment
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.common.KEY_THREE
import com.violas.wallet.common.KEY_TWO

/**
 * Created by elephant on 2020/8/28 17:24.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行币种存款/借款/清算记录公共视图
 */
abstract class BaseCoinTxRecordFragment<VO> : BasePagingFragment<VO>() {

    protected lateinit var mProductId: String
    protected lateinit var mWalletAddress: String
    protected lateinit var mCurrency: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!initData(savedInstanceState)) {
            finishActivity()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_ONE, mProductId)
        outState.putString(KEY_TWO, mWalletAddress)
        outState.putString(KEY_THREE, mCurrency)
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        try {
            val bundle = savedInstanceState ?: arguments ?: return false
            mProductId = bundle.getString(KEY_ONE, null) ?: return false
            mWalletAddress = bundle.getString(KEY_TWO, null) ?: return false
            mCurrency = bundle.getString(KEY_THREE, null) ?: return false
            return true
        } catch (e: Exception) {
            return false
        }
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)

        mPagingHandler.init()
        mPagingHandler.start()
    }
}