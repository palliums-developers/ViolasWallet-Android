package com.violas.wallet.ui.applyForMint

import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity

class ApplyForMintActivity
    : BaseAppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.hint_apply_for_mint)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_apply_for_mint
    }
}
