package com.violas.wallet.ui.main.market.swap

import com.palliums.base.BaseFragment
import com.violas.wallet.R

/**
 * Created by elephant on 2020/6/23 17:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 市场兑换视图
 */
class SwapFragment:BaseFragment() {

    private var lazyInitTag = false

    override fun getLayoutResId(): Int {
        return R.layout.fragment_swap
    }

    override fun onResume() {
        super.onResume()
        if (!lazyInitTag) {
            lazyInitTag = true
            onLazy2InitView()
        }
    }

    private fun onLazy2InitView() {

    }
}