package com.violas.wallet.ui.transfer

import android.os.Bundle
import android.text.AmountInputFilter
import com.palliums.extensions.expandTouchArea
import com.quincysx.crypto.CoinType
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsCommand
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.subscribeHub.BalanceSubscribeHub
import com.violas.wallet.repository.subscribeHub.BalanceSubscriber
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.main.market.bean.IAssetMark
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.utils.convertDisplayUnitToAmount
import com.violas.wallet.viewModel.bean.AssetVo
import kotlinx.android.synthetic.main.activity_transfer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * Diem/Violas转账页面
 */
class DiemTransferActivity : TransferActivity() {

    override fun getLayoutResId() = R.layout.activity_transfer

    private var mBalance: BigDecimal? = null

    private lateinit var mAsset: AssetVo

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
        if (mCoinNumber != getViolasCoinType().coinNumber() &&
            mCoinNumber != getDiemCoinType().coinNumber()
        ) {
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

        editAmountInput.filters = arrayOf(AmountInputFilter(12, 6))
    }

    private fun setBalance() {
        tvCoinAmount.text = String.format(
            getString(R.string.common_label_balance_format),
            mAsset.amountWithUnit.amount,
        )
    }

    private fun checkBalance(): Boolean {
        when (mPayerAccount?.coinNumber) {
            getDiemCoinType().coinNumber(),
            getViolasCoinType().coinNumber() -> {
                if (BigDecimal(
                        editAmountInput.text.toString().trim()
                    ) > mBalance ?: BigDecimal("0")
                ) {
                    LackOfBalanceException().message?.let { showToast(it) }
                    return false
                }
            }
        }
        return true
    }

    private fun send() {
        if (mPayerAccount == null) return

        val amount = editAmountInput.text.toString()
        val payeeAddress = editAddressInput.text.toString()
        try {
            mTransferManager.checkTransferParam(amount, payeeAddress, mPayerAccount!!)
            if (checkBalance()) {
                showPasswordSend(amount, payeeAddress)
            }
        } catch (e: Exception) {
            e.message?.let { it1 -> showToast(it1) }
            e.printStackTrace()
        }
    }

    private fun showPasswordSend(amount: String, payeeAddress: String) {
        if (mPayerAccount == null) return

        authenticateAccount(mPayerAccount!!) {
            launch(Dispatchers.IO) {
                mTransferManager.transfer(
                    this@DiemTransferActivity,
                    payeeAddress.trim(),
                    mPayeeSubAddress,
                    amount.trim(),
                    it,
                    mPayerAccount!!,
                    sbQuota.progress,
                    isToken,
                    mAsset.getId(),
                    {
                        print(it)
                        dismissProgress()
                        showToast(getString(R.string.transfer_tips_transfer_success))
                        CommandActuator.postDelay(RefreshAssetsCommand(), 2000)
                        finish()
                    },
                    {
                        it.printStackTrace()
                        dismissProgress()
                        showToast(it.message ?: getString(R.string.transfer_tips_transfer_failure))
                    })
            }
        }
    }
}
