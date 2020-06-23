package com.violas.wallet.ui.identity.createIdentity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import com.palliums.content.App
import com.palliums.extensions.expandTouchArea
import com.palliums.utils.*
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.ui.backup.BackupMnemonicFrom
import com.violas.wallet.ui.backup.BackupPromptActivity
import kotlinx.android.synthetic.main.activity_create_identity.*
import kotlinx.android.synthetic.main.activity_create_identity.tvPrivacyPolicy
import kotlinx.android.synthetic.main.activity_import_identity.*
import kotlinx.android.synthetic.main.activity_import_identity.btnConfirm
import kotlinx.android.synthetic.main.activity_import_identity.editConfirmPassword
import kotlinx.android.synthetic.main.activity_import_identity.editPassword
import kotlinx.android.synthetic.main.activity_wallet_connect_authorization.*
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_create_the_wallet)

        launch {
            tvPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance()
            tvPrivacyPolicy.text = buildUseBehaviorSpan()
            btnHasAgreePrivacyPolicy.expandTouchArea(28)
        }

        btnConfirm.setOnClickListener {
            if (!btnHasAgreePrivacyPolicy.isChecked) {
                showToast(getString(R.string.hint_wallet_agree_terms))
                return@setOnClickListener
            }
            val password = editPassword.text.toString().trim()
            val passwordConfirm = editConfirmPassword.text.toString().trim()

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
                        password.toByteArray()
                    )
                    withContext(Dispatchers.Main) {
                        dismissProgress()

                        BackupPromptActivity.start(
                            this@CreateIdentityActivity,
                            mnemonicWords as ArrayList<String>,
                            BackupMnemonicFrom.CREATE_IDENTITY_WALLET
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
            } catch (e: PasswordEmptyException) {
                showToast(getString(R.string.hint_please_password_not_empty))
            }
        }
    }

    private fun openWebPage(url: String) {
        Log.e("wallet connect", "open url $url")
        val webpage: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    private suspend fun buildUseBehaviorSpan() = withContext(Dispatchers.IO) {

        val useBehavior = getString(R.string.agreement_read_and_agree)
        val privacyPolicy = getString(R.string.wallet_privacy_policy_and_terms_of_service)
        val spannableStringBuilder = SpannableStringBuilder(useBehavior)

        val privacyPolicyClickSpanPrivacy = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openWebPage(getString(R.string.url_privacy_policy))
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = getColorByAttrId(
                    android.R.attr.textColorSecondary,
                    this@CreateIdentityActivity
                )
                ds.isUnderlineText = false//去掉下划线
            }
        }

        useBehavior.indexOf(privacyPolicy).also {
            spannableStringBuilder.setSpan(
                privacyPolicyClickSpanPrivacy,
                it,
                it + privacyPolicy.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        spannableStringBuilder
    }
}