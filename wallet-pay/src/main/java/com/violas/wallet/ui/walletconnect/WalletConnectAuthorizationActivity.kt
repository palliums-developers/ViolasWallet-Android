package com.violas.wallet.ui.walletconnect

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
import android.util.Log
import android.view.View
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.setSystemBar
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.walletconnect.WalletConnect
import com.violas.wallet.walletconnect.WalletConnectSessionListener
import com.violas.walletconnect.models.WCPeerMeta
import kotlinx.android.synthetic.main.activity_wallet_connect_authorization.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class WalletConnectAuthorizationActivity : BaseAppActivity() {

    companion object {
        private const val CONNECT_MSG = "connect_msg"

        fun start(context: Context, msg: String) {
            context.startActivity(
                Intent(
                    context,
                    WalletConnectAuthorizationActivity::class.java
                ).apply {
                    putExtra(CONNECT_MSG, msg)
                })
        }
    }

    // 是否处理了请求
    private var mRequestHandle = false

    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }

    override fun getLayoutResId(): Int {
        return R.layout.activity_wallet_connect_authorization
    }

    private val mWalletConnect by lazy {
        WalletConnect.getInstance(this.applicationContext)
    }

    private val mMsg by lazy {
        intent.getStringExtra(CONNECT_MSG)
    }

    override fun getTitleStyle() = PAGE_STYLE_NOT_TITLE

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.activity_none, R.anim.activity_bottom_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setSystemBar(lightModeStatusBar = true, lightModeNavigationBar = true)
        super.onCreate(savedInstanceState)

        launch {
            tvPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance()
            tvPrivacyPolicy.text = buildUseBehaviorSpan()
            tvPrivacyPolicy.highlightColor = Color.TRANSPARENT
        }

        if (mWalletConnect.isConnected()) {
            viewWalletConnectLoginWarning.visibility = View.VISIBLE
        } else {
            viewWalletConnectLoginWarning.visibility = View.INVISIBLE
        }
        mWalletConnect.mWalletConnectSessionListener = object : WalletConnectSessionListener {
            override fun onRequest(id: Long, peer: WCPeerMeta) {
                val findByCoinTypeByIdentity =
                    mAccountStorage.loadAllByCoinType(CoinTypes.Violas.coinType())
                val accounts =
                    findByCoinTypeByIdentity.map {
                        it.address
                    }
                val chainId = Vm.ViolasChainId.toString()
                if (mWalletConnect.approveSession(accounts, chainId)) {
                    mRequestHandle = true
                    dismissProgress()
                    close()
                } else {
                    dismissProgress()
                    showToast(String.format(getString(R.string.common_http_request_fail), ""))
                }
            }
        }
        btnConfirmLogin.setOnClickListener {
            showProgress()
            launch(Dispatchers.IO) {
                mWalletConnect.connect(mMsg)
            }
            btnConfirmLogin.postDelayed({
                dismissProgress()
                showToast(String.format(getString(R.string.common_http_request_fail), ""))
                launch(Dispatchers.IO) {
                    mWalletConnect.disconnect()
                }
            }, 10 * 1000)
        }
        tvCancelLogin.setOnClickListener {
            close()
        }
    }

    override fun onDestroy() {
        mWalletConnect.mWalletConnectSessionListener = null
//        if (!mRequestHandle) {
//            GlobalScope.launch {
//                mWalletConnect.rejectSession("reject")
//            }
//        }
        super.onDestroy()
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
        val useBehavior = getString(R.string.wallet_connect_login_declare)
        val privacyPolicy = getString(R.string.common_content_privacy_policy)
        val userAgreement = getString(R.string.common_content_user_agreement)
        val spannableStringBuilder = SpannableStringBuilder(useBehavior)
        val userAgreementClickSpanPrivacy = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openWebPage(getString(R.string.url_app_service_agreement))
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = getColorByAttrId(
                    R.attr.colorPrimary,
                    this@WalletConnectAuthorizationActivity
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
                    this@WalletConnectAuthorizationActivity
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
