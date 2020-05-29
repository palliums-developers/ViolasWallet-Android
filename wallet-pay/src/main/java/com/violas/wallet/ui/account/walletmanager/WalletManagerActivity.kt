package com.violas.wallet.ui.account.walletmanager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.biometric.BiometricManager
import androidx.fragment.app.Fragment
import com.palliums.biometric.BiometricCompat
import com.palliums.extensions.show
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.ChangeAccountNameEvent
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.event.WalletChangeEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.account.AccountInfoActivity
import com.violas.wallet.ui.backup.BackupMnemonicFrom
import com.violas.wallet.ui.backup.BackupPromptActivity
import com.violas.wallet.ui.backup.ShowMnemonicActivity
import com.violas.wallet.ui.biometric.CloseBiometricPaymentDialog
import com.violas.wallet.ui.biometric.CustomFingerprintDialog
import com.violas.wallet.ui.biometric.UnableBiometricPromptDialog
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.authenticateAccountByPassword
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

    private lateinit var mAccountDO: AccountDO
    private val mAccountManager by lazy {
        AccountManager()
    }
    private val mBiometricCompat by lazy {
        BiometricCompat.Builder(this).build()
    }

    override fun getLayoutResId() = R.layout.activity_wallet_manager

    override fun getTitleStyle(): Int {
        return TITLE_STYLE_GREY_BACKGROUND
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        title = getString(R.string.title_manager)

        val accountId = intent.getLongExtra(EXT_ACCOUNT_ID, -1L)
        launch(Dispatchers.IO) {
            val account = if (accountId == -1L) {
                mAccountManager.currentAccount()
            } else {
                mAccountManager.getAccountById(accountId)
            }

            mAccountDO = account

            withContext(Dispatchers.Main) {
                initView()
                initEvent()
            }
        }
    }

    private fun initView() {
        tvName.text = mAccountDO.walletNickname
        tvAddress.text = mAccountDO.address
        if (mAccountDO.walletType == 1) {
            btnRemoveWallet.visibility = View.VISIBLE
        }

        swtBtnBiometric.setCheckedImmediatelyNoEvent(mAccountDO.isOpenedBiometricPayment())
    }

    private fun initEvent() {
        layoutChangeName.setOnClickListener {
            AccountInfoActivity.start(this@WalletManagerActivity, mAccountDO.id)
        }

        layoutBack.setOnClickListener {
            authenticateAccount(
                mAccountDO,
                dismissLoadingWhenDecryptEnd = true,
                mnemonicCallback = {
                    backWallet(mAccountDO, it)
                }
            )
        }

        btnRemoveWallet.setOnClickListener {
            authenticateAccount(mAccountDO) {
                removeWallet(mAccountDO)
            }
        }

        clBiometric.setOnClickListener {
            swtBtnBiometric.isChecked = !swtBtnBiometric.isChecked
        }

        swtBtnBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!canBiometric()) {
                    swtBtnBiometric.setCheckedNoEvent(false)
                    return@setOnCheckedChangeListener
                }

                authenticateAccountByPassword(
                    mAccountDO,
                    dismissLoadingWhenDecryptEnd = true,
                    cancelCallback = {
                        swtBtnBiometric.setCheckedNoEvent(false)
                    },
                    passwordCallback = {
                        openBiometricPayment(it)
                    }
                )
            } else {
                closeBiometricPayment()
            }
        }
    }

    private fun canBiometric(): Boolean {
        val canAuthenticate = mBiometricCompat.canAuthenticate()
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            return true
        }

        val prompt = when (canAuthenticate) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                getString(R.string.desc_biometric_error_no_hardware)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                getString(R.string.desc_biometric_error_none_enrolled)
            }
            else -> {
                getString(R.string.desc_biometric_error_hardware_unavailable)
            }
        }
        UnableBiometricPromptDialog()
            .setPromptText(prompt)
            .show(supportFragmentManager)
        return false
    }

    private fun openBiometricPayment(password: String) {
        val promptParams =
            BiometricCompat.PromptParams.Builder(this)
                .title(getString(R.string.title_open_fingerprint_verification))
                .negativeButtonText(getString(R.string.action_cancel_nbsp))
                .positiveButtonText(getString(R.string.action_start_now_enable))
                .customFingerprintDialogClass(CustomFingerprintDialog::class.java)
                .reactivateBiometricWhenLockout(true)
                .build()

        val key = mAccountDO.getBiometricKey()
        mBiometricCompat.encrypt(promptParams, key, password) {
            if (it.type == BiometricCompat.Type.INFO) return@encrypt

            if (it.type == BiometricCompat.Type.SUCCESS) {
                launch(Dispatchers.Main) {
                    val encryptedPassword = it.value!!
                    mAccountDO.encryptedPassword = encryptedPassword.toByteArray()
                    withContext(Dispatchers.IO) {
                        mAccountManager.updateAccountPassword(mAccountDO.id, encryptedPassword)
                    }
                }
                return@encrypt
            }

            swtBtnBiometric.setCheckedNoEvent(false)
        }
    }

    private fun closeBiometricPayment() {
        CloseBiometricPaymentDialog()
            .setCallback(
                confirmCallback = {
                    mAccountDO.encryptedPassword = "".toByteArray()
                    launch(Dispatchers.IO) {
                        mAccountManager.updateAccountPassword(mAccountDO.id, "")
                    }
                },
                cancelCallback = {
                    swtBtnBiometric.setCheckedNoEvent(true)
                }
            ).show(supportFragmentManager)
    }

    private fun backWallet(account: AccountDO, mnemonics: List<String>) {
        if (account.walletType == 0 && !mAccountManager.isIdentityMnemonicBackup()) {
            BackupPromptActivity.start(
                this@WalletManagerActivity,
                mnemonics,
                BackupMnemonicFrom.BACKUP_IDENTITY_WALLET
            )
        } else {
            ShowMnemonicActivity.start(this@WalletManagerActivity, mnemonics)
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
            }
            EventBus.getDefault().post(WalletChangeEvent())
            withContext(Dispatchers.Main) {
                dismissProgress()
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

            mAccountDO = account

            withContext(Dispatchers.Main) {
                initView()
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
