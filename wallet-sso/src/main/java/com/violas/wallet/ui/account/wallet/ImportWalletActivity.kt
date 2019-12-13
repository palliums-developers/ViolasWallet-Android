package com.violas.wallet.ui.account.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.event.WalletChangeEvent
import kotlinx.android.synthetic.main.activity_create_wallet.*
import kotlinx.android.synthetic.main.activity_import_identity.*
import kotlinx.android.synthetic.main.activity_import_identity.btnConfirm
import kotlinx.android.synthetic.main.activity_import_identity.editConfirmPassword
import kotlinx.android.synthetic.main.activity_import_identity.editName
import kotlinx.android.synthetic.main.activity_import_identity.editPassword
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class ImportWalletActivity : BaseAppActivity() {
    companion object {
        private const val EXT_COIN_TYPE = "a1"

        fun start(context: Activity, coinType: CoinTypes, requestCode: Int = -1) {
            Intent(context, ImportWalletActivity::class.java)
                .apply {
                    putExtra(EXT_COIN_TYPE, coinType.coinType())
                }
                .start(context, requestCode)
        }
    }

    private lateinit var mCurrentCoinType: CoinTypes
    private val mAccountManager by lazy {
        AccountManager()
    }

    override fun getLayoutResId() = R.layout.activity_import_wallet
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = ""

        mCurrentCoinType =
            CoinTypes.parseCoinType(
                intent.getIntExtra(
                    EXT_COIN_TYPE,
                    CoinTypes.VToken.coinType()
                )
            )

        tvCreateHint.text =
            String.format(getString(R.string.hint_import_any_wallet), mCurrentCoinType.coinName())

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
            val mnemonic = editMnemonicWord.text.toString().trim()
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
                try {
                    val wordList = mnemonic.trim().split(" ")
                        .map { it.trim() }
                        .toList()
                    val newWallet = mAccountManager.importWallet(
                        mCurrentCoinType,
                        this@ImportWalletActivity,
                        wordList,
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
                } catch (e: Exception) {
                    dismissProgress()
                    showToast(getString(R.string.hint_mnemonic_error))
                }
            }
        }
    }
}
