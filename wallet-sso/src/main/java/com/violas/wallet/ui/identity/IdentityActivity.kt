package com.violas.wallet.ui.identity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.ui.identity.createIdentity.CreateIdentityActivity
import com.violas.wallet.ui.identity.importIdentity.ImportIdentityActivity
import kotlinx.android.synthetic.main.activity_identity.*

class IdentityActivity : BaseAppActivity() {
    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, IdentityActivity::class.java))
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_identity
    }

    override fun getPageStyle(): Int {
        return TITLE_STYLE_DARK_BACKGROUND_NO_TITLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        btnCreate.setOnClickListener {
            CreateIdentityActivity.start(this)
        }
        btnImport.setOnClickListener {
            ImportIdentityActivity.start(this)
        }
    }
}
