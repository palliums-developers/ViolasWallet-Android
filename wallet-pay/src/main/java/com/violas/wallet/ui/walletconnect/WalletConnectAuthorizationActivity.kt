package com.violas.wallet.ui.walletconnect

import android.app.Activity
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.palliums.content.App
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.walletconnect.WalletConnect
import com.violas.walletconnect.models.WCPeerMeta
import kotlinx.android.synthetic.main.activity_wallet_connect_authorization.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class WalletConnectAuthorizationActivity : BaseAppActivity() {

    companion object {
        private const val CONNECT_ID = "connect_id"
        private const val CONNECT_PEER_DATA = "connect_peer_data"

        private fun getContext(context: Context, result: (Boolean, Context) -> Unit) {
            var newTaskTag = false
            val contextWrapper: Context = when (context) {
                is App -> context.getTopActivity() ?: context.also { newTaskTag = true }
                is Activity -> context
                is Fragment -> {
                    if (context.activity == null) {
                        newTaskTag = true
                        context.applicationContext
                    } else {
                        context.activity!!
                    }
                }
                else -> context.also { newTaskTag = true }
            }
            return result.invoke(newTaskTag, contextWrapper)
        }

        fun startActivity(
            context: Context,
            id: Long,
            peer: WCPeerMeta
        ) {
            getContext(context) { newTaskTag, newContext ->
                newContext.startActivity(
                    Intent(
                        context,
                        WalletConnectAuthorizationActivity::class.java
                    ).apply {
                        if (newTaskTag) {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        putExtra(CONNECT_ID, id)
                        putExtra(CONNECT_PEER_DATA, peer)
                    })
            }
        }
    }

    // 是否处理了请求
    private var mRequestHandle = false

    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }

    override fun getLayoutResId(): Int {
        return R.layout.activity_wallet_connect_authorization
    }

    private val mWcClient by lazy {
        WalletConnect.getInstance(this.applicationContext).mWCClient
    }

    override fun getTitleStyle() = TITLE_STYLE_CUSTOM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitleLeftViewVisibility(View.GONE)

        launch(Dispatchers.IO) {
            val parcelableExtra = intent.getParcelableExtra<WCPeerMeta>(CONNECT_PEER_DATA)

            if (parcelableExtra != null) {
                withContext(Dispatchers.Main) {
                    tvScanLoginDescribe.text = String.format(
                        getString(R.string.desc_scan_wallet_connect_login_info),
                        parcelableExtra.name
                    )

                    tvPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance()
                    tvPrivacyPolicy.text = buildUseBehaviorSpan()

//                    tvScanLoginDescribe.text = parcelableExtra.description
                    if (parcelableExtra.icons.isNotEmpty()) {
                        Glide.with(this@WalletConnectAuthorizationActivity)
                            .load(parcelableExtra.icons[0])
                            .centerCrop()
                            .placeholder(R.drawable.ic_web)
                            .error(R.drawable.ic_web)
                            .into(ivDeskIcon)
                    }
                }
            } else {
                finish()
            }
        }

        btnConfirmLogin.setOnClickListener {
            launch {
                showProgress()
                withContext(Dispatchers.IO) {
                    val findByCoinTypeByIdentity =
                        mAccountStorage.loadAllByCoinType(CoinTypes.Violas.coinType())
                    val accounts =
                        findByCoinTypeByIdentity.map {
                            it.address
                        }
                    val chainId = if (Vm.TestNet) {
                        "violasTest"
                    } else {
                        "violas"
                    }
                    if (mWcClient.approveSession(accounts, chainId)) {
                        mRequestHandle = true
                        finish()
                    }
                }
                dismissProgress()
            }
        }
        tvCancelLogin.setOnClickListener {
            launch {
                showProgress()
                withContext(Dispatchers.IO) {
                    if (mWcClient.rejectSession("reject")) {
                        mRequestHandle = true
                        finish()
                    }
                }
                dismissProgress()
            }
        }
    }

    override fun onDestroy() {
        if (!mRequestHandle) {
            GlobalScope.launch {
                mWcClient.rejectSession("reject")
            }
        }
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
        val useBehavior = getString(R.string.wallet_connect_use_behavior)
        val privacyPolicy = getString(R.string.wallet_connect_privacy_policy)
        val userAgreement = getString(R.string.wallet_connect_user_agreement)
        val spannableStringBuilder = SpannableStringBuilder(useBehavior)
        val userAgreementClickSpanPrivacy = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openWebPage(getString(R.string.service_agreement_url))
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = ContextCompat.getColor(
                    this@WalletConnectAuthorizationActivity,
                    R.color.color_333333
                )
                ds.isUnderlineText = false//去掉下划线
            }
        }
        val privacyPolicyClickSpanPrivacy = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openWebPage(getString(R.string.url_privacy_policy))
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = ContextCompat.getColor(
                    this@WalletConnectAuthorizationActivity,
                    R.color.color_333333
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
