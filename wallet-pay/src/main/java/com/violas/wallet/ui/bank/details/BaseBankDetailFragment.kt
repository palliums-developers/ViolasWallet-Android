package com.violas.wallet.ui.bank.details

import android.os.Bundle
import android.view.View
import com.palliums.utils.getResourceId
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingFragment
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.common.KEY_TWO

/**
 * Created by elephant on 2020/8/28 17:24.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行存款/借款明细公共视图
 */
abstract class BaseBankDetailFragment<VO> : BasePagingFragment<VO>() {

    protected lateinit var mProductId: String
    protected lateinit var mWalletAddress: String

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
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        try {
            val bundle = savedInstanceState ?: arguments ?: return false
            mProductId = bundle.getString(KEY_ONE, null) ?: return false
            mWalletAddress = bundle.getString(KEY_TWO, null) ?: return false
            return true
        } catch (e: Exception) {
            return false
        }
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)

        mPagingHandler.init()
        getStatusLayout()?.setImageWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            getResourceId(R.attr.bankListEmptyDataBg, requireContext())
        )
        mPagingHandler.start()
    }
}