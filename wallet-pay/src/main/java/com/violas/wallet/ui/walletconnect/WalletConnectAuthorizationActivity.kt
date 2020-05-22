package com.violas.wallet.ui.walletconnect

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import kotlinx.coroutines.*

class WalletConnectAuthorizationActivity : BaseAppActivity() {

    companion object {
        private const val CONNECT_ID = "connect_id"
        private const val CONNECT_PEER_DATA = "connect_peer_data"

        private fun getContext(context: Context, result: (Boolean, Context) -> Unit) {
            var newTaskTag = false
            val contextWrapper: Context = when (context) {
                is App -> context.getTopActivity() ?: context.also { newTaskTag = true }
                is Activity -> context
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

    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }

    override fun getLayoutResId(): Int {
        return R.layout.activity_wallet_connect_authorization
    }

    private val mWcClient by lazy {
        WalletConnect.getInstance(this.applicationContext).mWCClient
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launch(Dispatchers.IO) {
            val parcelableExtra = intent.getParcelableExtra<WCPeerMeta>(CONNECT_PEER_DATA)
            if (parcelableExtra != null) {
                withContext(Dispatchers.Main) {
                    tvScanLoginDesc.text = String.format(
                        getString(R.string.desc_scan_wallet_connect_login),
                        parcelableExtra.name
                    )
                    tvScanLoginDescribe.text = parcelableExtra.description
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
                        finish()
                    }
                }
                dismissProgress()
            }
        }
    }
}
