package com.violas.wallet.ui.identity.createIdentity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
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
import kotlinx.android.synthetic.main.activity_import_identity.btnConfirm
import kotlinx.android.synthetic.main.activity_import_identity.editConfirmPassword
import kotlinx.android.synthetic.main.activity_import_identity.editPassword
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 创建钱包页面
 */
class CreateIdentityActivity : BaseAppActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, CreateIdentityActivity::class.java))
        }
    }

    override fun getLayoutResId() = R.layout.activity_create_identity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.create_wallet_title)

        launch {
            tvPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance()
            val buildUseBehaviorSpan = buildUseBehaviorSpan()
            tvPrivacyPolicy.text = buildUseBehaviorSpan
            tvPrivacyPolicy.highlightColor = Color.TRANSPARENT
            btnHasAgreePrivacyPolicy.expandTouchArea(28)
        }

        btnConfirm.setOnClickListener {
            if (!btnHasAgreePrivacyPolicy.isChecked) {
                showToast(getString(R.string.create_wallet_tips_1))
                return@setOnClickListener
            }

            val password = editPassword.text.toString().trim()
            val passwordConfirm = editConfirmPassword.text.toString().trim()

            try {
                PasswordCheckUtil.check(password)
            } catch (e: Exception) {
                showToast(e.message ?: getString(R.string.hint_please_minimum_password_length))
                return@setOnClickListener
            }

            if (!password.contentEquals(passwordConfirm)) {
                showToast(getString(R.string.create_wallet_tips_2))
                return@setOnClickListener
            }

            launch {
                showProgress()
                val mnemonicWords = withContext(Dispatchers.IO) {
                    AccountManager().createIdentity(
                        this@CreateIdentityActivity,
                        password.toByteArray()
                    )
                }
                dismissProgress()

                BackupPromptActivity.start(
                    this@CreateIdentityActivity,
                    mnemonicWords,
                    BackupMnemonicFrom.CREATE_IDENTITY_WALLET
                )

                App.finishAllActivity()
            }
        }
    }

    private fun openWebPage(url: String) {
        val webpage: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    private suspend fun buildUseBehaviorSpan() = withContext(Dispatchers.IO) {
        val useBehavior = getString(R.string.create_wallet_action_2)
        val userAgreement = getString(R.string.common_content_user_agreement)
        val privacyPolicy = getString(R.string.common_content_privacy_policy)
        val spannableStringBuilder = SpannableStringBuilder(useBehavior)
        val userAgreementClickSpanPrivacy = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openWebPage(getString(R.string.url_app_service_agreement))
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = getColorByAttrId(
                    R.attr.colorPrimary,
                    this@CreateIdentityActivity
                )
                ds.isUnderlineText = false//去掉下划线
            }
        }
        val privacyPolicyClickSpanPrivacy = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openWebPage(getString(R.string.url_app_privacy_policy))
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = getColorByAttrId(
                    R.attr.colorPrimary,
                    this@CreateIdentityActivity
                )
                ds.isUnderlineText = false//去掉下划线
            }
        }

        useBehavior.indexOf(userAgreement).also {
            spannableStringBuilder.setSpan(
                userAgreementClickSpanPrivacy,
                it,
                it + userAgreement.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
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