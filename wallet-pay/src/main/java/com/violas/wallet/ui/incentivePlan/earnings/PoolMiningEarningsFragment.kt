package com.violas.wallet.ui.incentivePlan.earnings

import android.os.Bundle
import com.palliums.base.BaseFragment
import com.violas.wallet.R
import com.violas.wallet.common.KEY_ONE

/**
 * Created by elephant on 11/26/20 2:28 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 资金池挖矿收益视图
 */
class PoolMiningEarningsFragment : BaseFragment() {

    companion object {
        fun newInstance(walletAddress: String): PoolMiningEarningsFragment {
            return PoolMiningEarningsFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_ONE, walletAddress)
                }
            }
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_pool_mining_earnings
    }

}