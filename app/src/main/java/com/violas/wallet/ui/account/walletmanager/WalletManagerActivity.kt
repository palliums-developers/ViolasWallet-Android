package com.violas.wallet.ui.account.walletmanager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.base.dialog.PasswordInputDialog
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.event.ChangeAccountNameEvent
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.account.AccountInfoActivity
import com.violas.wallet.ui.backup.ShowMnemonicActivity
import com.violas.wallet.utils.start
import kotlinx.android.synthetic.main.activity_wallet_manager.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class WalletManagerActivity : BaseActivity() {
    companion object {
        private const val EXT_ACCOUNT_ID = "b1"

        fun start(context: Activity, accountId: Long) {
            generateIntent(context, accountId).start(context)
        }

        fun start(context: Fragment, accountId: Long, requestCode: Int = -1) {
            context.context?.let {
                context.activity?.let { it1 -> generateIntent(it, accountId).start(it1) }
            }
        }

        private fun generateIntent(context: Context, accountId: Long): Intent {
            return Intent(context, WalletManagerActivity::class.java).apply {
                putExtra(EXT_ACCOUNT_ID, accountId)
            }
        }
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    override fun getLayoutResId() = R.layout.activity_wallet_manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        title = getString(R.string.title_manager)
        setTitleStyle(TITLE_STYLE_GREY_BACKGROUND)

        val accountId = intent.getLongExtra(EXT_ACCOUNT_ID, -1L)
        launch(Dispatchers.IO) {
            val account = if (accountId == -1L) {
                mAccountManager.currentAccount()
            } else {
                mAccountManager.getAccountById(accountId)
            }

            if (account.walletType == 1) {
                btnRemoveWallet.visibility = View.VISIBLE
            }

            withContext(Dispatchers.Main) {
                tvName.text = account.walletNickname
                tvAddress.text = account.address

                layoutChangeName.setOnClickListener {
                    AccountInfoActivity.start(this@WalletManagerActivity, accountId)
                }

                layoutBack.setOnClickListener {
                    PasswordInputDialog()
                        .setConfirmListener { password, dialog ->
                            backWallet(account, password)
                            dialog.dismiss()
                        }
                        .show(supportFragmentManager)
                }

                btnRemoveWallet.setOnClickListener {
                    PasswordInputDialog()
                        .setConfirmListener { password, dialog ->
                            removeWallet(account)
                            dialog.dismiss()
                        }
                        .show(supportFragmentManager)
                }
            }
        }
    }

    private fun backWallet(account: AccountDO, password: ByteArray) {
        launch(Dispatchers.IO) {
            try {
                val decrypt = SimpleSecurity.instance(this@WalletManagerActivity.applicationContext)
                    .decrypt(password, account.mnemonic)
                if (decrypt == null) {
                    showToast(getString(R.string.hint_password_error))
                    return@launch
                }
                val decryptStr = String(decrypt)
                val mnemonic = decryptStr.substring(1, decryptStr.length - 1)
                    .split(",")
                    .map { it.trim() }
                    .toMutableList() as ArrayList
                ShowMnemonicActivity.start(this@WalletManagerActivity, mnemonic, true)
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    private fun removeWallet(account: AccountDO) {
        showProgress()
        launch(Dispatchers.IO) {
            val removeAccountID = account.id
            val currentAccountID = mAccountManager.currentAccount().id
            mAccountManager.removeWallet(account)
            if (removeAccountID == currentAccountID) {
                mAccountManager.switchCurrentAccount()
                EventBus.getDefault().post(SwitchAccountEvent())
            }
            withContext(Dispatchers.Main) {
                finish()
            }
        }
    }

    private fun refresh() {
        val accountId = intent.getLongExtra(EXT_ACCOUNT_ID, -1L)
        launch(Dispatchers.IO) {
            val account = if (accountId == -1L) {
                mAccountManager.currentAccount()
            } else {
                mAccountManager.getAccountById(accountId)
            }

            withContext(Dispatchers.Main) {
                tvName.text = account.walletNickname
                tvAddress.text = account.address
            }
        }
    }

    @Subscribe
    fun onChangeAccountNameEvent(event: ChangeAccountNameEvent) {
        refresh()
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }
}
