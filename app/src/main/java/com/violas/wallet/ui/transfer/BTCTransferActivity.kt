package com.violas.wallet.ui.transfer

import android.accounts.AccountsException
import android.os.Bundle
import android.widget.SeekBar
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
                account?.let {
                    mTransactionManager = TransactionManager(arrayListOf(it.address))
                    mTransactionManager.setFeeCallback {
                        this@BTCTransferActivity.runOnUiThread {
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
                    try {
                        if (amount > 0) {
                            val convertAmountToDisplayUnit =
                                convertAmountToDisplayUnit(amount, CoinTypes.Bitcoin)
                            editAmountInput.setText(convertAmountToDisplayUnit.first)
                        }
                        title = "${parseCoinType.coinName()}${getString(R.string.transfer)}"
                        tvHintCoinName.text = parseCoinType.coinName()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: AccountsException) {
                e.printStackTrace()
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
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
        sbQuota.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                account?.apply {
                    launch(Dispatchers.IO) {
                        try {
                            mTransactionManager.transferProgressIntent(sbQuota.progress)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        })
        editAmountInput.addTextChangedListener {
            account?.apply {
                launch(Dispatchers.IO) {
                    try {
                        mTransactionManager.transferAmountIntent(
                            it.toString().toDouble()
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun refreshCurrentAmount() {
        account?.let {
            mAccountManager.getBalanceWithUnit(it) { balance, unit ->
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
                    launch(Dispatchers.IO) {
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
