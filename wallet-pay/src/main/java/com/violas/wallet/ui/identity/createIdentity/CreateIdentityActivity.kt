package com.violas.wallet.ui.identity.createIdentity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.content.App
import com.palliums.utils.*
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.ui.backup.BackupMnemonicFrom
import com.violas.wallet.ui.backup.BackupPromptActivity
import kotlinx.android.synthetic.main.activity_import_identity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateIdentityActivity : BaseAppActivity() {
    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, CreateIdentityActivity::class.java))
        }
    }

    override fun getLayoutResId() = R.layout.activity_create_identity

    override fun getTitleStyle(): Int {
        return TITLE_STYLE_MAIN_COLOR
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_create_the_wallet)
        btnConfirm.setOnClickListener {
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
                    val mnemonicWords = AccountManager().createIdentity(
                        this@CreateIdentityActivity,
                        walletName,
                        password.toByteArray()
                    )
                    withContext(Dispatchers.Main) {
                        dismissProgress()

                        BackupPromptActivity.start(
                            this@CreateIdentityActivity,
                            mnemonicWords as ArrayList<String>,
                            BackupMnemonicFrom.CREATE_IDENTITY
                        )

                        App.finishAllActivity()
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
            }
        }
    }

}