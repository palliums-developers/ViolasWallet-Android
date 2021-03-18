package com.violas.wallet.ui.transfer

import android.accounts.AccountsException
import android.os.Bundle
import android.text.AmountInputFilter
import androidx.lifecycle.Observer
import com.palliums.extensions.expandTouchArea
import com.quincysx.crypto.CoinType
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.database.entity.AccountType
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.utils.convertDisplayUnitToAmount
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsVo
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
    private lateinit var mAssetsVo: AssetsVo

    private val mWalletAppViewModel by lazy {
        WalletAppViewModel.getInstance()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        toAddress = editAddressInput.text.toString().trim()
        transferAmount = convertDisplayUnitToAmount(
            editAmountInput.text.toString().trim(),
            CoinType.parseCoinNumber(coinNumber)
        )
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CommandActuator.post(RefreshAssetsAllListCommand())
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
                    account = AccountManager.getAccountById(mAssetsVo.getAccountId())
                    if (account?.accountType == AccountType.NoDollars && mAssetsVo is AssetsCoinVo) {
                        showToast(getString(R.string.transfer_tips_unsupported_currency))
                        finish()
                        return@launch
                    }
                    refreshCurrentAmount()

                    val coinType = CoinType.parseCoinNumber(account!!.coinNumber)
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
                } catch (e: AccountsException) {
                    finish()
                }
            }
        })
        initViewData()
        editAmountInput.filters = arrayOf(AmountInputFilter(12, 6))
        ivScan.setOnClickListener {
            ScanActivity.start(this, REQUEST_SCAN_QR_CODE)
        }

        btnConfirm.setOnClickListener {
            send()
        }
        ivAddressBook.setOnClickListener {
            account?.coinNumber?.let { it1 ->
                AddressBookActivity.start(
                    this@DiemTransferActivity,
                    it1,
                    true,
                    REQUEST_SELECTOR_ADDRESS
                )
            }
        }
        ivAddressBook.expandTouchArea(8)
    }

    private fun initViewData() {
        editAddressInput.setText(toAddress)
    }

    private suspend fun refreshCurrentAmount() {
        withContext(Dispatchers.Main) {
            mAssetsVo.amountWithUnit
            tvCoinAmount.text = String.format(
                getString(R.string.transfer_label_balance_format),
                mAssetsVo.amountWithUnit.amount,
                mAssetsVo.amountWithUnit.unit
            )
        }

        mBalance = BigDecimal(mAssetsVo.getAmount())
    }

    private fun checkBalance(): Boolean {
        when (account?.coinNumber) {
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
        val amount = editAmountInput.text.toString()
        val address = editAddressInput.text.toString()

        if (account == null) {
            return
        }
        try {
            mTransferManager.checkTransferParam(amount, address, account!!)

            if (!checkBalance()) {
                return
            }

            showPasswordSend(amount, address)
        } catch (e: Exception) {
            e.message?.let { it1 -> showToast(it1) }
            e.printStackTrace()
        }
    }

    private fun showPasswordSend(amount: String, address: String) {
        if (account == null) return

        authenticateAccount(account!!) {
            launch(Dispatchers.IO) {
                mTransferManager.transfer(
                    this@DiemTransferActivity,
                    address.trim(),
                    toSubAddress,
                    amount.trim(),
                    it,
                    account!!,
                    sbQuota.progress,
                    isToken,
                    mAssetsVo.getId(),
                    {
                        print(it)
                        dismissProgress()
                        showToast(getString(R.string.transfer_tips_transfer_success))
                        CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
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

    override fun onSelectAddress(address: String) {
        editAddressInput.setText(address)
    }

    override fun onScanAddressQr(address: String) {
        editAddressInput.setText(address)
    }
}
