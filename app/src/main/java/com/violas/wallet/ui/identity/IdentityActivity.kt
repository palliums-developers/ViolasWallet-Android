package com.violas.wallet.ui.identity

import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.ui.identity.createIdentity.CreateIdentityActivity
import com.violas.wallet.ui.identity.importIdentity.ImportIdentityActivity
import kotlinx.android.synthetic.main.activity_identity.*

class IdentityActivity : BaseActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_identity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("身份管理")

        btnCreate.setOnClickListener {
            CreateIdentityActivity.start(this)
        }
        btnImport.setOnClickListener {
            ImportIdentityActivity.start(this)
        }
    }

}
