package com.violas.wallet.base

import android.os.Bundle
import androidx.lifecycle.Observer
import com.palliums.base.BaseViewModel
import com.palliums.net.LoadState

/**
 * Created by elephant on 2019-11-25 14:04.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
abstract class BaseViewModelActivity : BaseAppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getViewModel().loadState.observe(this, Observer {
            when (it.peekData().status) {
                LoadState.Status.RUNNING -> {
                    showProgress()
                }

                else -> {
                    dismissProgress()
                }
            }
        })

        getViewModel().tipsMessage.observe(this, Observer { wrapper ->
            wrapper.getDataIfNotHandled()?.let {
                if (it.isNotEmpty()) {
                    showToast(it)
                }
            }
        })
    }

    abstract fun getViewModel(): BaseViewModel
}