package com.violas.wallet.ui.identity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.WalletType
import com.violas.wallet.ui.identity.createIdentity.CreateIdentityActivity
import com.violas.wallet.ui.identity.importIdentity.ImportIdentityActivity
import com.violas.wallet.ui.main.MainActivity
import kotlinx.android.synthetic.main.activity_identity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IdentityActivity : BaseAppActivity() {
    private var mCurrentTypeWallet = WalletType.Governor

    companion object {
        private const val EXT_WALLET_TYPE = "ext_wallet_type"

        fun start(context: Context, walletType: WalletType = WalletType.Governor) {
            Intent(context, IdentityActivity::class.java).apply {
                putExtra(EXT_WALLET_TYPE, walletType.type)
            }.start(context)
        }
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_identity
    }

    override fun getPageStyle(): Int {
        return PAGE_STYLE_DARK_BACKGROUND_NO_TITLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mCurrentTypeWallet =
            WalletType.parse(intent.getIntExtra(EXT_WALLET_TYPE, WalletType.Governor.type))

        initViewData()

        btnCreate.setOnClickListener {
            if (mCurrentTypeWallet == WalletType.Governor) {
                HintGovernorDialog()
                    .setConfirmListener {
                        it.dismiss()
                        CreateIdentityActivity.start(this, mCurrentTypeWallet)
                    }
                    .show(supportFragmentManager)
            } else {
                CreateIdentityActivity.start(this, mCurrentTypeWallet)
            }
        }
        btnImport.setOnClickListener {
            ImportIdentityActivity.start(this, mCurrentTypeWallet)
        }
        btnExchange.setOnClickListener {
            launch(Dispatchers.IO) {
                when (mCurrentTypeWallet) {
                    WalletType.Governor -> {
                        handleChangeWallet(WalletType.SSO)
                    }
                    WalletType.SSO -> {
                        handleChangeWallet(WalletType.Governor)
                    }
                }
            }
        }
    }

    private fun handleChangeWallet(walletType: WalletType) {
        val account =
            mAccountManager.getIdentityByWalletType(walletType.type)
        if (account == null) {
            mCurrentTypeWallet = walletType
            initViewData()
            if (walletType == WalletType.Governor) {
                showToast(R.string.hint_exchange_governor_identity)
            } else if (walletType == WalletType.SSO) {
                showToast(R.string.hint_exchange_sso_identity)
            }
        } else {
            mAccountManager.switchCurrentAccount(account.id)
            MainActivity.start(this@IdentityActivity)
        }
    }

    private fun initViewData() {
        when (mCurrentTypeWallet) {
            WalletType.Governor -> {
                btnCreate.setText(R.string.create_governor_identity)
                btnImport.setText(R.string.import_governor_identity)
                btnExchange.setText(R.string.exchange_sso_identity)
            }
            WalletType.SSO -> {
                btnCreate.setText(R.string.create_sso_identity)
                btnImport.setText(R.string.import_sso_identity)
                btnExchange.setText(R.string.exchange_governor_identity)
            }
        }
    }
}
