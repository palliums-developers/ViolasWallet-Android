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
        title = getString(R.string.title_create_the_wallet)
        setTitleStyle(TITLE_STYLE_MAIN_COLOR)
        btnConfirm.setOnClickListener {
            val walletName = editName.text.toString().trim()
            val password = editPassword.text.toString().trim().toByteArray()
            val passwordConfirm = editConfirmPassword.text.toString().trim().toByteArray()

            if (walletName.isEmpty()) {
                showToast(getString(R.string.hint_nickname_empty))
                return@setOnClickListener
            }
            if (editPassword.text.toString().length < 6) {
                showToast(getString(R.string.hint_input_password_short))
                return@setOnClickListener
            }
            if (!password.contentEquals(passwordConfirm)) {
                showToast(getString(R.string.hint_confirm_password_fault))
                return@setOnClickListener
            }

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