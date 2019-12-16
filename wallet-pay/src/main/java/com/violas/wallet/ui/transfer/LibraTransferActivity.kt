package com.violas.wallet.ui.transfer

import android.accounts.AccountsException
import android.os.Bundle
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.event.RefreshBalanceEvent
import com.violas.wallet.repository.database.entity.TokenDo
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.widget.dialog.PasswordInputDialog
import kotlinx.android.synthetic.main.activity_transfer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.math.BigDecimal
import java.math.RoundingMode

class LibraTransferActivity : TransferActivity() {
    override fun getLayoutResId() = R.layout.activity_transfer

    private val mTokenManager by lazy {
        TokenManager()
    }

    private var mTokenDo: TokenDo? = null
    private var mBalance: BigDecimal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_transfer)
        accountId = intent.getLongExtra(EXT_ACCOUNT_ID, 0)
        tokenId = intent.getLongExtra(EXT_TOKEN_ID, 0)
        isToken = intent.getBooleanExtra(EXT_IS_TOKEN, false)
        launch(Dispatchers.IO) {
            try {
                account = mAccountManager.getAccountById(accountId)
                mTokenDo = mTokenManager.findTokenById(tokenId)
                refreshCurrentAmount()
                val amount = intent.getLongExtra(
                    EXT_AMOUNT,
                    0
                )

                val parseCoinType = CoinTypes.parseCoinType(account!!.coinNumber)
                withContext(Dispatchers.Main) {
                    if (amount > 0) {
                        val convertAmountToDisplayUnit =
                            convertAmountToDisplayUnit(amount, parseCoinType)
                        editAmountInput.setText(convertAmountToDisplayUnit.first)
                    }
                    if (isToken) {
                        title = "${mTokenDo?.name ?: ""} ${getString(R.string.transfer)}"
                        tvHintCoinName.text = mTokenDo?.name ?: ""
                    } else {
                        title = "${parseCoinType.coinName()} ${getString(R.string.transfer)}"
                        tvHintCoinName.text = parseCoinType.coinName()
                    }
                }
            } catch (e: AccountsException) {
                finish()
            }
        }
        initViewData()

        ivScan.setOnClickListener {
            ScanActivity.start(this, REQUEST_SCAN_QR_CODE)
        }

        btnConfirm.setOnClickListener {
            send()
        }
        tvAddressBook.setOnClickListener {
            account?.coinNumber?.let { it1 ->
                AddressBookActivity.start(
                    this@LibraTransferActivity,
                    it1,
                    true,
                    REQUEST_SELECTOR_ADDRESS
                )
            }
        }
    }

    private fun refreshCurrentAmount() {
        account?.let {
            if (isToken) {
                mTokenDo?.apply {
                    mTokenManager.getTokenBalance(it.address, tokenAddress) { balance ->
                        launch {
                            val balanceStr = BigDecimal(balance.toString()).divide(
                                BigDecimal("1000000"),
                                6,
                                RoundingMode.HALF_UP
                            ).stripTrailingZeros().toPlainString()
                            tvCoinAmount.text = String.format(
                                getString(R.string.hint_transfer_amount),
                                balanceStr,
                                name
                            )
                            mBalance = BigDecimal(balanceStr)
                        }
                    }
                }
            } else {
                mAccountManager.getBalanceWithUnit(it) { balance, unit ->
                    launch {
                        tvCoinAmount.text = String.format(
                            getString(R.string.hint_transfer_amount),
                            balance,
                            unit
                        )
                        mBalance = BigDecimal(balance)
                    }
                }
            }
        }
    }

    private fun send() {
        val amount = editAmountInput.text.toString().trim().toDouble()
        val address = editAddressInput.text.toString().trim()
        if (amount <= 0) {
            showToast(getString(R.string.hint_please_input_amount))
            return
        }
        if (address.isEmpty()) {
            showToast(getString(R.string.hint_please_input_address))
            return
        }
        when (account?.coinNumber) {
            CoinTypes.Libra.coinType(),
            CoinTypes.Violas.coinType() -> {
                if (BigDecimal(editAmountInput.text.toString().trim()) >= mBalance) {
                    LackOfBalanceException().message?.let { showToast(it) }
                    return
                }
            }
        }

        PasswordInputDialog()
            .setConfirmListener { bytes, dialogFragment ->
                dialogFragment.dismiss()
                account?.let {
                    showProgress()
                    launch(Dispatchers.IO) {
                        mTransferManager.transfer(
                            this@LibraTransferActivity,
                            address,
                            amount,
                            bytes,
                            account!!,
                            sbQuota.progress,
                            isToken,
                            tokenId,
                            {
                                EventBus.getDefault().post(RefreshBalanceEvent())
                                dismissProgress()
                                print(it)
                                finish()
                            },
                            {
                                it.message?.let { it1 -> showToast(it1) }
                                dismissProgress()
                                it.printStackTrace()
                            })
                    }
                }
            }.show(supportFragmentManager)
    }

    private fun initViewData() {
        editAddressInput.setText(intent.getStringExtra(EXT_ADDRESS))
    }

    override fun onSelectAddress(address: String) {
        editAddressInput.setText(address)
    }

    override fun onScanAddressQr(address: String) {
        editAddressInput.setText(address)
    }
}
