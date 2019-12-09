package com.violas.wallet.ui.dexOrder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.repository.http.dex.DexOrderDTO

/**
 * Created by elephant on 2019-12-09 11:34.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 订单详情
 */
class DexOrderDetailsActivity : BaseAppActivity() {

    companion object {
        private const val EXTRA_KEY_DEX_ORDER = "EXTRA_KEY_DEX_ORDER"

        fun start(context: Context, dexOrderDTO: DexOrderDTO) {
            val intent = Intent(context, DexOrderDetailsActivity::class.java)
                .apply {
                    putExtra(EXTRA_KEY_DEX_ORDER, dexOrderDTO)
                }
            context.startActivity(intent)
        }
    }

    private var dexOrderDTO: DexOrderDTO? = null

    override fun getLayoutResId(): Int {
        return R.layout.activity_dex_order_details
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            dexOrderDTO = savedInstanceState.getParcelable(EXTRA_KEY_DEX_ORDER)
        } else if (intent != null) {
            dexOrderDTO = intent.getParcelableExtra(EXTRA_KEY_DEX_ORDER)
        }

        if (dexOrderDTO == null) {
            finish()
            return
        }

        setTitle(R.string.title_order_details)

        loadRootFragment(
            R.id.flFragmentContainer,
            DexOrdersFragment.newInstance(null, dexOrderDTO!!.tokenGet, dexOrderDTO!!.tokenGive)
        )
    }
}