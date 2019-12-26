package com.violas.wallet.ui.identity.importIdentity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.content.App
import com.palliums.utils.*
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
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
            val password = editPassword.text.toString().trim()
            val passwordConfirm = editConfirmPassword.text.toString().trim()

            if (walletName.isEmpty()) {
                showToast(getString(R.string.hint_nickname_empty))
                return@setOnClickListener
            }

            try {
                PasswordCheckUtil.check(password)

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
                            password.toByteArray()
                        )
                        accountManager.setIdentityMnemonicBackup()
                        withContext(Dispatchers.Main) {
                            dismissProgress()
                            MainActivity.start(this@ImportIdentityActivity)
                            App.finishAllActivity()
                        }
                    } catch (e: Exception) {
                        dismissProgress()
                        showToast(getString(R.string.hint_mnemonic_error))
                        e.printStackTrace()
                    }
                }
            } catch (e: PasswordLengthShortException) {
                showToast(getString(R.string.hint_please_minimum_password_length))
            } catch (e: PasswordLengthLongException) {
                showToast(getString(R.string.hint_please_maxmum_password_length))
            } catch (e: PasswordSpecialFailsException) {
                showToast(getString(R.string.hint_please_cannot_contain_special_characters))
            } catch (e: PasswordValidationFailsException) {
                showToast(getString(R.string.hint_please_password_rules_are_wrong))
            } catch (e: PasswordEmptyException) {
                showToast(getString(R.string.hint_please_password_not_empty))
            }
        }
    }

}
