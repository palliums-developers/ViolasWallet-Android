package com.violas.wallet.ui.account

import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseFragment

/**
 * Created by elephant on 2019-10-23 17:35.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 账户操作视图
 * 提供两种操作模式：[AccountOperationMode.SELECTION]、[AccountOperationMode.MANAGEMENT]
 */
class AccountOperationFragment : BaseFragment() {

    companion object {
        private const val INTENT_KEY_OPERATION_MODE = "INTENT_KEY_OPERATION_MODE"
        private const val INTENT_KEY_DISPLAY_MODE = "INTENT_KEY_DISPLAY_MODE"
    }

    var operationMode: Int = -1
    var displayMode: Int = -1

    override fun getLayoutResId(): Int {
        return R.layout.fragment_account_operation
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        var bundle: Bundle? = savedInstanceState ?: arguments
        bundle?.let {
            operationMode = it.getInt(INTENT_KEY_OPERATION_MODE, -1)
            displayMode = it.getInt(INTENT_KEY_DISPLAY_MODE, -1)
        }


    }
}