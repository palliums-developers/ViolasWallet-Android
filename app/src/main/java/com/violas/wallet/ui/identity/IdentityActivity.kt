package com.violas.wallet.ui.identity

import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity

class IdentityActivity : BaseActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_identity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("身份管理")
    }

}
