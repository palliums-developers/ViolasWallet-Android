package com.violas.wallet.ui.transfer

import android.accounts.AccountsException
import android.os.Bundle
import android.text.AmountInputFilter
import android.util.Log
import android.widget.SeekBar
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.android.synthetic.main.activity_transfer.*
import kotlinx.android.synthetic.main.activity_transfer_btc.*
import kotlinx.android.synthetic.main.activity_transfer_btc.btnConfirm
import kotlinx.android.synthetic.main.activity_transfer_btc.editAddressInput
import kotlinx.android.synthetic.main.activity_transfer_btc.editAmountInput
import kotlinx.android.synthetic.main.activity_transfer_btc.ivScan
import kotlinx.android.synthetic.main.activity_transfer_btc.sbQuota
import kotlinx.android.synthetic.main.activity_transfer_btc.tvAddressBook
import kotlinx.android.synthetic.main.activity_transfer_btc.tvCoinAmount
import kotlinx.android.synthetic.main.activity_transfer_btc.tvFee
import kotlinx.android.synthetic.main.activity_transfer_btc.tvHintCoinName
import kotlinx.coroutines.*
import java.math.BigDecimal

class BTCTransferActivity : TransferActivity() {

    //BTC
    private lateinit var mTransactionManager: TransactionManager
    private lateinit var mAssetsVo: AssetsVo

    override fun getLayoutResId() = R.layout.activity_transfer_btc

    private val mWalletAppViewModel by lazy {
        WalletAppViewModel.getViewModelInstance(this@BTCTransferActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_transfer)
        assetsName = intent.getStringExtra(EXT_ASSETS_NAME)
        coinNumber = intent.getIntExtra(EXT_COIN_NUMBER, CoinTypes.Violas.coinType())
        isToken = intent.getBooleanExtra(EXT_IS_TOKEN, false)
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
            Log.e("=======", exists.toString())
            if (!exists) {
                showToast(getString(R.string.hint_unsupported_tokens))
                finish()
            }
            launch(Dispatchers.IO) {
                try {
                    account = mAccountManager.getAccountById(mAssetsVo.getAccountId())
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
                            title = "${mAssetsVo.getAssetsName()} ${getString(R.string.transfer)}"
                            tvHintCoinName.text = mAssetsVo.getAssetsName()
                        } else {
                            title = "${parseCoinType.coinName()} ${getString(R.string.transfer)}"
                            tvHintCoinName.text = parseCoinType.coinName()
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

    private suspend fun refreshCurrentAmount() {
        mWalletAppViewModel.refreshAssetsList()
        withContext(Dispatchers.Main) {
            mAssetsVo.amountWithUnit
            tvCoinAmount.text = String.format(
                getString(R.string.hint_transfer_amount),
                mAssetsVo.amountWithUnit.amount,
                mAssetsVo.amountWithUnit.unit
            )
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
                        showToast(getString(R.string.hint_transfer_broadcast_success))
                        dismissProgress()
                        GlobalScope.launch {
                            // todo 刷新单币种价格
                            // todo 优化成命令模式
                            delay(3000)
                            mWalletAppViewModel.refreshAssetsList()
                        }
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
        editAddressInput.setText(intent.getStringExtra(EXT_ADDRESS))
    }

    override fun onSelectAddress(address: String) {
        editAddressInput.setText(address)
    }

    override fun onScanAddressQr(address: String) {
        editAddressInput.setText(address)
    }
}
