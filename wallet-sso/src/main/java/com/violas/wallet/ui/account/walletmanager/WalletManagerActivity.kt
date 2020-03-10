package com.violas.wallet.ui.account.walletmanager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.WalletType
import com.violas.wallet.event.ChangeAccountNameEvent
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.event.WalletChangeEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.account.AccountInfoActivity
import com.violas.wallet.ui.backup.BackupMnemonicFrom
import com.violas.wallet.ui.backup.BackupPromptActivity
import com.violas.wallet.ui.backup.ShowMnemonicActivity
import com.violas.wallet.utils.showPwdInputDialog
import kotlinx.android.synthetic.main.activity_wallet_manager.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class WalletManagerActivity : BaseAppActivity() {
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

    override fun getPageStyle(): Int {
        return PAGE_STYLE_PLIGHT_TITLE_SLIGHT_CONTENT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        title = getString(R.string.title_manager)

        val accountId = intent.getLongExtra(EXT_ACCOUNT_ID, -1L)
        launch(Dispatchers.IO) {
            val accountDO = if (accountId == -1L) {
                mAccountManager.currentAccount()
            } else {
                mAccountManager.getAccountById(accountId)
            }

            // 州长钱包或SSO钱包都不能删除
            /*if (account.walletType == 1) {
                btnRemoveWallet.visibility = View.VISIBLE
            }*/

            withContext(Dispatchers.Main) {
                tvName.text = accountDO.walletNickname
                tvAddress.text = accountDO.address

                layoutChangeName.setOnClickListener {
                    AccountInfoActivity.start(this@WalletManagerActivity, accountId)
                }

                layoutBack.setOnClickListener {
                    showPwdInputDialog(
                        accountDO,
                        mnemonicCallback = {
                            dismissProgress()
                            backupWallet(accountDO, it)
                        })
                }

                btnRemoveWallet.setOnClickListener {
                    showPwdInputDialog(
                        accountDO,
                        accountCallback = {
                            dismissProgress()
                            removeWallet(accountDO)
                        }
                    )
                }
            }
        }
    }

    private fun backupWallet(accountDO: AccountDO, mnemonics: List<String>) {
        val walletType = WalletType.parse(accountDO.walletType)
        if (!mAccountManager.isMnemonicBackup(walletType)) {
            BackupPromptActivity.start(
                this@WalletManagerActivity,
                mnemonics as ArrayList<String>,
                if (walletType == WalletType.Governor)
                    BackupMnemonicFrom.BACKUP_GOVERNOR_WALLET
                else
                    BackupMnemonicFrom.BACKUP_SSO_WALLET
            )
        } else {
            ShowMnemonicActivity.start(this, mnemonics as ArrayList<String>)
        }
    }

    private fun removeWallet(account: AccountDO) {
        launch(Dispatchers.IO) {
            val removeAccountID = account.id
            val currentAccountID = mAccountManager.currentAccount().id
            mAccountManager.removeWallet(account)
            if (removeAccountID == currentAccountID) {
                mAccountManager.switchCurrentAccount()
                EventBus.getDefault().post(SwitchAccountEvent())
                EventBus.getDefault().post(WalletChangeEvent())
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
