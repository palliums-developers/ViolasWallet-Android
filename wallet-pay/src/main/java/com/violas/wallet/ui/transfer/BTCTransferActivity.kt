package com.violas.wallet.ui.transfer

import android.accounts.AccountsException
import android.os.Bundle
import android.text.AmountInputFilter
import android.widget.SeekBar
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import com.palliums.extensions.expandTouchArea
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.utils.convertDisplayUnitToAmount
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.android.synthetic.main.activity_transfer_btc.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BTC转账页面
 */
class BTCTransferActivity : TransferActivity() {

    //BTC
    private lateinit var mTransactionManager: TransactionManager
    private lateinit var mAssetsVo: AssetsVo

    override fun getLayoutResId() = R.layout.activity_transfer_btc

    private val mWalletAppViewModel by lazy {
        WalletAppViewModel.getViewModelInstance(this@BTCTransferActivity)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        toAddress = editAddressInput.text.toString().trim()
        transferAmount = convertDisplayUnitToAmount(
            editAmountInput.text.toString().trim(),
            CoinTypes.parseCoinType(coinNumber)
        )
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mWalletAppViewModel.mAssetsListLiveData.observe(this, Observer {
            var exists = false
            for (item in it) {
                val isCoinTrue =
                    assetsName == null && item is AssetsCoinVo && item.getCoinNumber() == coinNumber
                val isTokenTrue =
                    assetsName != null && item !is AssetsCoinVo && item.getCoinNumber() == coinNumber && item.getAssetsName() == assetsName
                if (isCoinTrue || isTokenTrue) {
                    mAssetsVo = item
                    exists = true
                    break
                }
            }
            if (!exists) {
                showToast(getString(R.string.transfer_tips_unopened_or_unsupported_token))
                finish()
                return@Observer
            }

            launch(Dispatchers.IO) {
                try {
                    account = mAccountManager.getAccountById(mAssetsVo.getAccountId())

                    val coinType = CoinTypes.parseCoinType(account!!.coinNumber)
                    withContext(Dispatchers.Main) {
                        if (transferAmount > 0) {
                            val convertAmountToDisplayUnit =
                                convertAmountToDisplayUnit(transferAmount, coinType)
                            editAmountInput.setText(convertAmountToDisplayUnit.first)
                        }
                        if (isToken) {
                            title =
                                getString(R.string.transfer_title_format, mAssetsVo.getAssetsName())
                            tvHintCoinName.text = mAssetsVo.getAssetsName()
                        } else {
                            title =
                                getString(R.string.transfer_title_format, coinType.coinName())
                            tvHintCoinName.text = coinType.coinName()
                        }
                    }

                    account?.let {
                        mTransactionManager = TransactionManager(arrayListOf(it.address))
                        mTransactionManager.setFeeCallback {
                            this@BTCTransferActivity.runOnUiThread {
                                tvFee.text = "$it BTC"
                            }
                        }
                    }
                } catch (e: AccountsException) {
                    finish()
                }
            }
        })
        initViewData()

        ivScan.setOnClickListener {
            ScanActivity.start(this, REQUEST_SCAN_QR_CODE)
        }

        btnConfirm.setOnClickListener {
            send()
        }
        ivAddressBook.setOnClickListener {
            account?.coinNumber?.let { it1 ->
                AddressBookActivity.start(
                    this@BTCTransferActivity,
                    it1,
                    true,
                    REQUEST_SELECTOR_ADDRESS
                )
            }
        }
        ivAddressBook.expandTouchArea(8)
        editAmountInput.filters = arrayOf(AmountInputFilter(9, 8))
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

    private fun send() {
        val amount = editAmountInput.text.toString()
        val address = editAddressInput.text.toString()

        if (account == null) {
            return
        }
        try {
            mTransferManager.checkTransferParam(amount, address, account!!)

            showPasswordSend(amount, address)
        } catch (e: Exception) {
            e.message?.let { it1 -> showToast(it1) }
            e.printStackTrace()
        }
    }

    private fun showPasswordSend(amount: String, address: String) {
        if (account == null) return
        authenticateAccount(account!!, mAccountManager) {
            launch(Dispatchers.IO) {
                mTransferManager.transferBtc(
                    mTransactionManager,
                    address.trim(),
                    amount.trim().toDouble(),
                    it,
                    account!!,
                    sbQuota.progress,
                    success = {
                        showToast(getString(R.string.transfer_tips_transfer_success))
                        dismissProgress()
                        CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
                        print(it)
                        finish()
                    },
                    error = {
                        it.message?.let { it1 -> showToast(it1) }
                        dismissProgress()
                        it.printStackTrace()
                    })
            }
        }
    }

    private fun initViewData() {
        editAddressInput.setText(toAddress)
    }

    override fun onSelectAddress(address: String) {
        editAddressInput.setText(address)
    }

    override fun onScanAddressQr(address: String) {
        editAddressInput.setText(address)
    }
}
