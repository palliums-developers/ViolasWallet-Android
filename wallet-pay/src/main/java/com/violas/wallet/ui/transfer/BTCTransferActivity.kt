package com.violas.wallet.ui.transfer

import android.os.Bundle
import android.text.AmountInputFilter
import android.widget.SeekBar
import androidx.core.widget.addTextChangedListener
import com.palliums.extensions.expandTouchArea
import com.quincysx.crypto.CoinType
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsCommand
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.repository.subscribeHub.BalanceSubscribeHub
import com.violas.wallet.repository.subscribeHub.BalanceSubscriber
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.main.market.bean.IAssetMark
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.utils.convertDisplayUnitToAmount
import com.violas.wallet.viewModel.bean.AssetVo
import kotlinx.android.synthetic.main.activity_transfer_btc.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BTC转账页面
 */
class BTCTransferActivity : TransferActivity() {

    private val mTransactionManager by lazy {
        TransactionManager(arrayListOf(mPayerAccount!!.address)).apply {
            setFeeCallback {
                launch {
                    tvFee.text = "$it ${CoinType.parseCoinNumber(mCoinNumber).coinName()}"
                }
            }
        }
    }

    private lateinit var mAsset: AssetVo

    override fun getLayoutResId() = R.layout.activity_transfer_btc

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mPayeeAddress = editAddressInput.text.toString().trim()
        mAmount = convertDisplayUnitToAmount(
            editAmountInput.text.toString().trim(),
            CoinType.parseCoinNumber(mCoinNumber)
        )
        super.onSaveInstanceState(outState)
    }

    override fun onSelectAddress(address: String) {
        editAddressInput.setText(address)
    }

    override fun onScanAddressQr(address: String) {
        editAddressInput.setText(address)
    }

    private fun init() {
        if (mCoinNumber != getBitcoinCoinType().coinNumber()) {
            close()
            return
        }

        val assetMark = IAssetMark.convert(mCoinNumber, mCurrency)
        val balanceSubscriber = object : BalanceSubscriber(assetMark) {
            override fun onNotice(asset: AssetVo?) {
                launch {
                    // 已初始化，资产更新
                    if (mPayerAccount != null) {
                        if (asset != null) {
                            mAsset = asset
                            setBalance()
                        }
                        return@launch
                    }

                    // 未找到对应资产
                    if (asset == null) {
                        showToast(getString(R.string.transfer_tips_unopened_or_unsupported_token))
                        close()
                        return@launch
                    }

                    val account = withContext(Dispatchers.IO) {
                        try {
                            AccountManager.getAccountById(asset.getAccountId())
                        } catch (e: Exception) {
                            null
                        }
                    }
                    // 未找到账户
                    if (account == null) {
                        showToast(getString(R.string.common_tips_account_error))
                        close()
                        return@launch
                    }

                    // 找到资产，初始化
                    mPayerAccount = account
                    mAsset = asset
                    initView()
                    initEvent()
                    setBalance()
                    CommandActuator.post(RefreshAssetsCommand())
                }
            }
        }
        BalanceSubscribeHub.observe(this, balanceSubscriber)
    }

    private fun initView() {
        title = getString(R.string.transfer_title_format, mAsset.getAssetsName())
        tvHintCoinName.text = mAsset.getAssetsName()
        if (!mPayeeAddress.isNullOrBlank())
            editAddressInput.setText(mPayeeAddress)
        if (mAmount > 0) {
            val displayAmount =
                convertAmountToDisplayUnit(mAmount, CoinType.parseCoinNumber(mCoinNumber))
            editAmountInput.setText(displayAmount.first)
        }
    }

    private fun initEvent() {
        ivScan.setOnClickListener {
            ScanActivity.start(this, REQUEST_SCAN_QR_CODE)
        }

        btnConfirm.setOnClickListener {
            send()
        }

        ivAddressBook.expandTouchArea(8)
        ivAddressBook.setOnClickListener {
            AddressBookActivity.start(
                this,
                mCoinNumber,
                true,
                REQUEST_SELECTOR_ADDRESS
            )
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
                launch(Dispatchers.IO) {
                    try {
                        mTransactionManager.transferProgressIntent(sbQuota.progress)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })

        editAmountInput.filters = arrayOf(AmountInputFilter(9, 8))
        editAmountInput.addTextChangedListener {
            launch(Dispatchers.IO) {
                try {
                    mTransactionManager.transferAmountIntent(it.toString().toDouble())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setBalance() {
        tvCoinAmount.text = getString(
            R.string.common_label_balance_format,
            mAsset.amountWithUnit.amount
        )
    }

    private fun send() {
        if (mPayerAccount == null) return

        val amount = editAmountInput.text.toString()
        val address = editAddressInput.text.toString()
        try {
            mTransferManager.checkTransferParam(amount, address, mPayerAccount!!)
            showPasswordSend(amount, address)
        } catch (e: Exception) {
            e.message?.let { it1 -> showToast(it1) }
            e.printStackTrace()
        }
    }

    private fun showPasswordSend(amount: String, address: String) {
        if (mPayerAccount == null) return

        authenticateAccount(mPayerAccount!!) {
            launch(Dispatchers.IO) {
                mTransferManager.transferBtc(
                    mTransactionManager,
                    address.trim(),
                    amount.trim().toDouble(),
                    it,
                    mPayerAccount!!,
                    sbQuota.progress,
                    success = {
                        print(it)
                        dismissProgress()
                        showToast(getString(R.string.transfer_tips_transfer_success))
                        CommandActuator.postDelay(RefreshAssetsCommand(), 2000)
                        finish()
                    },
                    error = {
                        it.printStackTrace()
                        dismissProgress()
                        showToast(it.message ?: getString(R.string.transfer_tips_transfer_failure))
                    })
            }
        }
    }
}
