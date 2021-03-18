package com.violas.wallet.ui.identity.importIdentity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.*
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import kotlinx.android.synthetic.main.activity_import_identity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 导入钱包页面
 */
class ImportIdentityActivity : BaseAppActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ImportIdentityActivity::class.java))
        }
    }

    override fun getLayoutResId() = R.layout.activity_import_identity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.import_wallet_title)
        btnConfirm.setOnClickListener {
            val mnemonic = editMnemonicWord.text.toString().trim()
            val password = editPassword.text.toString().trim()
            val passwordConfirm = editConfirmPassword.text.toString().trim()

            try {
                PasswordCheckUtil.check(password)
            } catch (e: Exception) {
                showToast(e.message ?: getString(R.string.hint_please_minimum_password_length))
                return@setOnClickListener
            }

            if (!password.contentEquals(passwordConfirm)) {
                showToast(getString(R.string.import_wallet_tips_two_pwd_not_equals))
                return@setOnClickListener
            }

            showProgress()
            launch(Dispatchers.IO) {
                try {
                    val wordList = mnemonic.trim().split(" ")
                        .map { it.trim() }
                        .toList()
                    AccountManager.importIdentityWallet(
                        wordList,
                        password.toByteArray()
                    )
                    AccountManager.setIdentityMnemonicBackup()
                    withContext(Dispatchers.Main) {
                        dismissProgress()
                        finish()
//                            MainActivity.start(this@ImportIdentityActivity)
//                            App.finishAllActivity()
                    }
                } catch (e: Exception) {
                    dismissProgress()
                    showToast(getString(R.string.import_wallet_tips_mnemonics_error))
                    e.printStackTrace()
                }
            }
        }
    }

}
