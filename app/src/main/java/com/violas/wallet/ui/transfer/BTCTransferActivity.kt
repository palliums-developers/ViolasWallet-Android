package com.violas.wallet.ui.transfer

import android.accounts.AccountsException
import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.dialog.PasswordInputDialog
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.utils.convertAmountToDisplayUnit
import kotlinx.android.synthetic.main.activity_transfer_btc.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BTCTransferActivity : TransferActivity() {

    //BTC
    private lateinit var mTransactionManager: TransactionManager

    override fun getLayoutResId() = R.layout.activity_transfer_btc

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_transfer)
        accountId = intent.getLongExtra(EXT_ACCOUNT_ID, 0)
        launch(Dispatchers.IO) {
            try {
                account = mAccountManager.getAccountById(accountId)
                refreshCurrentAmount()
                account?.apply {
                    mTransactionManager = TransactionManager(arrayListOf(address))
                    mTransactionManager.setFeeCallback {
                        launch {
                            tvFee.text = "$it BTC"
                        }
                    }
                }

                val amount = intent.getLongExtra(
                    EXT_AMOUNT,
                    0
                )

                val parseCoinType = CoinTypes.parseCoinType(account!!.coinNumber)
                withContext(Dispatchers.Main) {
                    if (amount > 0) {
                        val convertAmountToDisplayUnit =
                            convertAmountToDisplayUnit(amount, CoinTypes.Bitcoin)
                        editAmountInput.setText(convertAmountToDisplayUnit.first)
                    }
                    title = "${parseCoinType.coinName()}${getString(R.string.transfer)}"
                    tvHintCoinName.text = parseCoinType.coinName()
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
                    this@BTCTransferActivity,
                    it1,
                    true,
                    REQUEST_SELECTOR_ADDRESS
                )
            }
        }
        editAmountInput.addTextChangedListener {
            account?.apply {
                if (coinNumber == CoinTypes.Bitcoin.coinType() || coinNumber == CoinTypes.BitcoinTest.coinType()) {
                    try {
                        mTransactionManager.checkBalance(
                            it.toString().toDouble(),
                            2,
                            sbQuota.progress
                        )
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    private fun refreshCurrentAmount() {
        account?.let {
            mAccountManager.getBalance(it) { balance, unit ->
                launch {
                    tvCoinAmount.text = String.format(
                        getString(R.string.hint_transfer_amount),
                        balance,
                        unit
                    )
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
        PasswordInputDialog()
            .setConfirmListener { bytes, dialogFragment ->
                dialogFragment.dismiss()
                account?.let {
                    showProgress()
                    mTransferManager.transferBtc(
                        mTransactionManager,
                        address,
                        amount,
                        bytes,
                        account!!,
                        sbQuota.progress,
                        {
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
