package com.violas.wallet.ui.account.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.ui.backup.BackupMnemonicFrom
import com.violas.wallet.ui.backup.BaseBackupMnemonicActivity
import com.violas.wallet.ui.backup.ShowMnemonicActivity
import com.violas.wallet.utils.start
import kotlinx.android.synthetic.main.activity_create_wallet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class CreateWalletActivity : BaseActivity() {
    companion object {
        private const val REQUEST_BACK_MNEMONIC = 1
        private const val EXT_COIN_TYPE = "a1"

        fun start(context: Activity, coinType: CoinTypes, requestCode: Int = -1) {
            Intent(context, CreateWalletActivity::class.java)
                .apply {
                    putExtra(EXT_COIN_TYPE, coinType.coinType())
                }
                .start(context, requestCode)
        }
    }

    private val mAccountManager by lazy {
        AccountManager()
    }
    private lateinit var mGenerateWalletMnemonic: ArrayList<String>
    private lateinit var mCurrentCoinType: CoinTypes
    override fun getLayoutResId() = R.layout.activity_create_wallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = ""

        mCurrentCoinType =
            CoinTypes.parseCoinType(intent.getIntExtra(EXT_COIN_TYPE, CoinTypes.VToken.coinType()))

        tvCreateHint.text =
            String.format(getString(R.string.hint_create_any_wallet), mCurrentCoinType.coinName())

        ivLogo.setImageResource(
            when (mCurrentCoinType) {
                CoinTypes.BitcoinTest, CoinTypes.Bitcoin -> {
                    R.drawable.icon_bitcoin_big
                }
                CoinTypes.Libra -> {
                    R.drawable.icon_libra_big
                }
                CoinTypes.VToken -> {
                    R.drawable.icon_violas_big
                }
                else -> {
                    R.drawable.icon_bitcoin_big
                }
            }
        )

        btnConfirm.setOnClickListener {
            val walletName = editName.text.toString().trim()
            val password = editPassword.text.toString().trim().toByteArray()
            val passwordConfirm = editConfirmPassword.text.toString().trim().toByteArray()

            if (walletName.isEmpty()) {
                showToast(getString(R.string.hint_nickname_empty))
                return@setOnClickListener
            }
            if (editPassword.text.toString().length < 6) {
                showToast(getString(R.string.hint_input_password_short))
                return@setOnClickListener
            }
            if (!password.contentEquals(passwordConfirm)) {
                showToast(getString(R.string.hint_confirm_password_fault))
                return@setOnClickListener
            }

            showProgress()
            launch(Dispatchers.IO) {
                mGenerateWalletMnemonic = mAccountManager.generateWalletMnemonic()
                dismissProgress()
                Intent(this@CreateWalletActivity, ShowMnemonicActivity::class.java).apply {
                    putStringArrayListExtra(
                        BaseBackupMnemonicActivity.INTENT_KET_MNEMONIC_WORDS,
                        mGenerateWalletMnemonic
                    )
                    putExtra(
                        BaseBackupMnemonicActivity.INTENT_KET_MNEMONIC_FROM,
                        BackupMnemonicFrom.CREATE_IDENTITY
                    )
                }.start(this@CreateWalletActivity, REQUEST_BACK_MNEMONIC)
            }
        }
    }

    private fun saveWallet() {
        val walletName = editName.text.toString().trim()
        val password = editPassword.text.toString().trim().toByteArray()
        showProgress()
        launch(Dispatchers.IO) {
            val newWallet = AccountManager().importWallet(
                mCurrentCoinType,
                this@CreateWalletActivity,
                mGenerateWalletMnemonic,
                walletName,
                password
            )
            mAccountManager.switchCurrentAccount(newWallet)
            withContext(Dispatchers.Main) {
                dismissProgress()
                EventBus.getDefault().post(SwitchAccountEvent())
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_BACK_MNEMONIC) {
            if (resultCode == Activity.RESULT_OK) {
                saveWallet()
            }
        }
    }
}
