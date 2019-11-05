package com.violas.wallet.base

import androidx.annotation.StringRes

/**
 * Created by elephant on 2019-11-05 18:30.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface ViewController {

    fun showProgress(@StringRes resId: Int)

    fun showProgress(msg: String? = null)

    fun dismissProgress()

    fun showToast(@StringRes msgId: Int)

    fun showToast(msg: String)

    /**
     * 加载使用对话框，返回false则使用SmartRefreshLayout的加载View
     */
    fun loadingUseDialog() = true
}