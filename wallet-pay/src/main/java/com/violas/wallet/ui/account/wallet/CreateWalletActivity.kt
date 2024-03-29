package com.violas.wallet.ui.account.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.*
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.event.WalletChangeEvent
import com.violas.wallet.ui.backup.BackupMnemonicFrom
import com.violas.wallet.ui.backup.BackupPromptActivity
import kotlinx.android.synthetic.main.activity_create_wallet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class CreateWalletActivity : BaseAppActivity() {
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
            CoinTypes.parseCoinType(intent.getIntExtra(EXT_COIN_TYPE, CoinTypes.Violas.coinType()))

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
                CoinTypes.Violas -> {
                    R.drawable.icon_violas_big
                }
                else -> {
                    R.drawable.icon_bitcoin_big
                }
            }
        )

        btnConfirm.setOnClickListener {
            val walletName = editName.text.toString().trim()
            val password = editPassword.text.toString().trim()
            val passwordConfirm = editConfirmPassword.text.toString().trim()

            if (walletName.isEmpty()) {
                showToast(getString(R.string.hint_nickname_empty))
                return@setOnClickListener
            }
            try {
                PasswordCheckUtil.check(password)
                if (!password.contentEquals(passwordConfirm)) {
                    showToast(getString(R.string.hint_confirm_password_fault))
                    return@setOnClickListener
                }
                showProgress()
                launch(Dispatchers.IO) {
                    mGenerateWalletMnemonic = mAccountManager.generateWalletMnemonic()

                    dismissProgress()

                    BackupPromptActivity.start(
                        this@CreateWalletActivity,
                        mGenerateWalletMnemonic,
                        BackupMnemonicFrom.CREATE_OTHER_WALLET,
                        REQUEST_BACK_MNEMONIC
                    )
                }
            } catch (e: PasswordLengthShortException) {
                showToast(getString(R.string.hint_please_minimum_password_length))
            } catch (e: PasswordLengthLongException) {
                showToast(getString(R.string.hint_please_maxmum_password_length))
            } catch (e: PasswordSpecialFailsException) {
                showToast(getString(R.string.hint_please_cannot_contain_special_characters))
            } catch (e: PasswordValidationFailsException) {
                showToast(getString(R.string.hint_please_password_rules_are_wrong))
            } catch (e: PasswordEmptyException) {
                showToast(getString(R.string.hint_please_password_not_empty))
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
                EventBus.getDefault().post(WalletChangeEvent())
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_BACK_MNEMONIC) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)
                saveWallet()
            }
        }
    }
}
