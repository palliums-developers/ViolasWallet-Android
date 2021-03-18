package com.violas.wallet.ui.account.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.*
import com.quincysx.crypto.CoinType
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.event.WalletChangeEvent
import kotlinx.android.synthetic.main.activity_import_wallet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class ImportWalletActivity : BaseAppActivity() {
    companion object {
        private const val EXT_COIN_TYPE = "a1"

        fun start(context: Activity, coinType: CoinType, requestCode: Int = -1) {
            Intent(context, ImportWalletActivity::class.java)
                .apply {
                    putExtra(EXT_COIN_TYPE, coinType.coinNumber())
                }
                .start(context, requestCode)
        }
    }

    private lateinit var mCurrentCoinType: CoinType

    override fun getLayoutResId() = R.layout.activity_import_wallet
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = ""

        mCurrentCoinType =
            CoinType.parseCoinNumber(
                intent.getIntExtra(
                    EXT_COIN_TYPE,
                    getViolasCoinType().coinNumber()
                )
            )

        tvCreateHint.text =
            String.format(getString(R.string.init_wallet_title_import_wallet_format), mCurrentCoinType.coinName())

        ivLogo.setImageResource(
            when (mCurrentCoinType) {
                getBitcoinCoinType() -> {
                    R.drawable.ic_bitcoin_big
                }
                getDiemCoinType() -> {
                    R.drawable.ic_libra_big
                }
                getViolasCoinType() -> {
                    R.drawable.ic_violas_big
                }
                else -> {
                    R.drawable.ic_bitcoin_big
                }
            }
        )

        btnConfirm.setOnClickListener {
            val mnemonic = editMnemonicWord.text.toString().trim()
            val walletName = editName.text.toString().trim()
            val password = editPassword.text.toString().trim()
            val passwordConfirm = editConfirmPassword.text.toString().trim()

            if (walletName.isEmpty()) {
                showToast(getString(R.string.init_wallet_tips_wallet_name_empty))
                return@setOnClickListener
            }
            try {
                PasswordCheckUtil.check(password)
                if (!password.contentEquals(passwordConfirm)) {
                    showToast(getString(R.string.init_wallet_tips_two_pwd_not_equals))
                    return@setOnClickListener
                }
                showProgress()
                launch(Dispatchers.IO) {
                    try {
                        val wordList = mnemonic.trim().split(" ")
                            .map { it.trim() }
                            .toList()
                        val newWallet = AccountManager.importNonIdentityWallet(
                            mCurrentCoinType,
                            wordList,
                            password.toByteArray(),
                            walletName
                        )
                        AccountManager.switchCurrentAccount(newWallet)
                        withContext(Dispatchers.Main) {
                            dismissProgress()
                            EventBus.getDefault().post(SwitchAccountEvent())
                            EventBus.getDefault().post(WalletChangeEvent())
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    } catch (e: Exception) {
                        dismissProgress()
                        showToast(getString(R.string.import_wallet_tips_mnemonics_error))
                        e.printStackTrace()
                    }
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
}
