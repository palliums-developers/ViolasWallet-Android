package com.violas.wallet.ui.identity.createIdentity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.violas.wallet.App
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.ui.main.MainActivity
import kotlinx.android.synthetic.main.activity_import_identity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateIdentityActivity : BaseActivity() {
    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, CreateIdentityActivity::class.java))
        }
    }

    override fun getLayoutResId() = R.layout.activity_create_identity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("导入钱包")

        btnConfirm.setOnClickListener {
            val walletName = editName.text.toString().trim()
            val password = editPassword.text.toString().trim().toByteArray()
            showProgress()
            launch(Dispatchers.IO) {
                AccountManager().createIdentity(
                    this@CreateIdentityActivity,
                    walletName,
                    password
                )
                withContext(Dispatchers.Main) {
                    dismissProgress()
                    App.finishAllActivity()
                    MainActivity.start(this@CreateIdentityActivity)
                }
            }
        }
    }

}