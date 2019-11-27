package com.violas.wallet.ui.mintSuccess

import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity

class MintSuccessActivity : BaseAppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_mint_success)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_mint_success
    }
}
