package com.violas.wallet.ui.dexOrder

import android.os.Bundle
import androidx.annotation.StringDef
import com.palliums.base.BaseFragment
import com.violas.wallet.R

/**
 * Created by elephant on 2019-12-06 12:02.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易中心订单
 */

@StringDef(
    DexOrdersType.OPEN,
    DexOrdersType.FILLED,
    DexOrdersType.CANCELED,
    DexOrdersType.FINISHED
)
annotation class DexOrdersType {
    companion object {
        const val OPEN = "0"        // open
        const val FILLED = "1"      // filled
        const val CANCELED = "2"    // canceled
        const val FINISHED = "3"    // finished（filled and canceled）
    }
}

class DexOrdersFragment : BaseFragment() {

    companion object {
        private const val EXTRA_KEY_ORDER_TYPE = "EXTRA_KEY_ORDER_TYPE"

        fun newInstance(@DexOrdersType orderType: String): DexOrdersFragment {
            val bundle = Bundle().apply {
                putString(EXTRA_KEY_ORDER_TYPE, orderType)
            }

            return DexOrdersFragment().apply {
                arguments = bundle
            }
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_dex_orders
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)
    }
}