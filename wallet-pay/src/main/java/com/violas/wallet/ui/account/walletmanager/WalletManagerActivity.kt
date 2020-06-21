package com.violas.wallet.ui.account.walletmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.biometric.BiometricManager
import com.palliums.biometric.BiometricCompat
import com.palliums.extensions.show
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.event.HomePageType
import com.violas.wallet.event.SwitchHomePageEvent
import com.violas.wallet.event.WalletChangeEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.backup.BackupMnemonicFrom
import com.violas.wallet.ui.backup.BackupPromptActivity
import com.violas.wallet.ui.biometric.CloseBiometricsDialog
import com.violas.wallet.ui.biometric.CustomFingerprintDialog
import com.violas.wallet.ui.biometric.UnableBiometricPromptDialog
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.authenticateAccountByPassword
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.activity_wallet_manager.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

/**
 * 钱包管理页面，删除钱包后回到钱包首页
 */
class WalletManagerActivity : BaseAppActivity() {

    companion object {
        fun start(context: Context) {
            Intent(context, WalletManagerActivity::class.java).start(context)
        }
    }

    private lateinit var mAccountDO: AccountDO
    private val mAccountManager by lazy {
        AccountManager()
    }
    private val mWalletAppViewModel by lazy {
        WalletAppViewModel.getViewModelInstance(this)
    }
    private val mBiometricCompat by lazy {
        BiometricCompat.Builder(this).build()
    }

    override fun getLayoutResId() = R.layout.activity_wallet_manager

    override fun getTitleStyle(): Int {
        return PAGE_STYLE_SECONDARY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_manager)

        launch {
            try {
                mAccountDO = mAccountManager.getDefaultAccount()
                initView()
                initEvent()
            } catch (e: Exception) {
                close()
            }
        }
    }

    private fun initView() {
        swtBtnBiometric.setCheckedImmediatelyNoEvent(
            !mAccountManager.getSecurityPassword().isNullOrBlank()
        )
    }

    private fun initEvent() {
        clExportMnemonics.setOnClickListener {
            authenticateAccount(
                mAccountDO,
                mAccountManager,
                dismissLoadingWhenDecryptEnd = true,
                mnemonicCallback = {
                    exportMnemonics(it)
                }
            )
        }

        btnDeleteWallet.setOnClickListener {
            DeleteWalletPromptDialog().setCallback {
                authenticateAccount(mAccountDO, mAccountManager) {
                    deleteWallet()
                }
            }.show(supportFragmentManager)
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
                        openBiometricAuthentication(it)
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

    private fun openBiometricAuthentication(password: String) {
        val promptParams =
            BiometricCompat.PromptParams.Builder(this)
                .title(getString(R.string.title_open_fingerprint_verification))
                .negativeButtonText(getString(R.string.action_cancel_nbsp))
                .positiveButtonText(getString(R.string.action_start_now_enable))
                .customFingerprintDialogClass(CustomFingerprintDialog::class.java)
                .reactivateWhenLockoutPermanent(true)
                .autoCloseWhenError(false)
                .build()

        val key = AccountManager.getBiometricKey()
        mBiometricCompat.encrypt(promptParams, key, password) {
            if (it.type == BiometricCompat.Type.INFO) return@encrypt

            if (it.type == BiometricCompat.Type.SUCCESS) {
                mAccountManager.updateSecurityPassword(it.value!!)
                return@encrypt
            }

            swtBtnBiometric.setCheckedNoEvent(false)
            if (it.reason == BiometricCompat.Reason.USER_CANCELED
                || it.reason == BiometricCompat.Reason.CANCELED
                || it.reason == BiometricCompat.Reason.NEGATIVE_BUTTON
            ) {
                mBiometricCompat.cancel()
            }
        }
    }

    private fun closeBiometricPayment() {
        CloseBiometricsDialog()
            .setCallback(
                confirmCallback = {
                    mAccountManager.updateSecurityPassword("")
                },
                cancelCallback = {
                    swtBtnBiometric.setCheckedNoEvent(true)
                }
            ).show(supportFragmentManager)
    }

    private fun exportMnemonics(mnemonics: List<String>) {
        BackupPromptActivity.start(
            this@WalletManagerActivity,
            mnemonics,
            BackupMnemonicFrom.BACKUP_IDENTITY_WALLET
        )
    }

    private fun deleteWallet() {
        launch {
            withContext(Dispatchers.IO) {
                // 删除本地所有的account和token
                mAccountManager.deleteAllAccount()
                TokenManager().deleteAllToken()
                // 清除本地配置
                mAccountManager.clearLocalConfig()
            }

            // 发送删除钱包事件，通知钱包首页刷新UI
            EventBus.getDefault().post(WalletChangeEvent())
            EventBus.getDefault().post(SwitchHomePageEvent(HomePageType.Wallet))
            mWalletAppViewModel.refreshAssetsList()

            delay(500)

            dismissProgress()
            finish()
        }
    }
}
