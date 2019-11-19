package com.violas.wallet.ui.identity.importIdentity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.content.App
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.MnemonicException
import com.violas.wallet.ui.main.MainActivity
import kotlinx.android.synthetic.main.activity_import_identity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImportIdentityActivity : BaseAppActivity() {
    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ImportIdentityActivity::class.java))
        }
    }

    override fun getLayoutResId() = R.layout.activity_import_identity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_import_the_wallet)
        btnConfirm.setOnClickListener {
            val mnemonic = editMnemonicWord.text.toString().trim()
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
                try {
                    val accountManager = AccountManager()
                    val wordList = mnemonic.trim().split(" ")
                        .map { it.trim() }
                        .toList()
                    accountManager.importIdentity(
                        this@ImportIdentityActivity,
                        wordList,
                        walletName,
                        password
                    )
                    accountManager.setIdentityMnemonicBackup()
                    withContext(Dispatchers.Main) {
                        dismissProgress()
                        MainActivity.start(this@ImportIdentityActivity)
                        App.finishAllActivity()
                    }
                } catch (e: MnemonicException) {
                    showToast(getString(R.string.hint_mnemonic_error))
                }
            }
        }
    }

}
