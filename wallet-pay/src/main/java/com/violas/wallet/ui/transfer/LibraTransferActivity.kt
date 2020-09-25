package com.violas.wallet.ui.transfer

import android.accounts.AccountsException
import android.os.Bundle
import android.text.AmountInputFilter
import androidx.lifecycle.Observer
import com.palliums.extensions.expandTouchArea
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
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

class LibraTransferActivity : TransferActivity() {
    override fun getLayoutResId() = R.layout.activity_transfer

    private var mBalance: BigDecimal? = null
    private lateinit var mAssetsVo: AssetsVo

    private val mWalletAppViewModel by lazy {
        WalletAppViewModel.getViewModelInstance(this@LibraTransferActivity)
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
                showToast(getString(R.string.hint_unsupported_tokens))
                finish()
                return@Observer
            }

            launch(Dispatchers.IO) {
                try {
                    account = mAccountManager.getAccountById(mAssetsVo.getAccountId())
                    if (account?.accountType == AccountType.NoDollars && mAssetsVo is AssetsCoinVo) {
                        showToast(getString(R.string.hint_unsupported_coin))
                        finish()
                        return@launch
                    }
                    refreshCurrentAmount()

                    val coinType = CoinTypes.parseCoinType(account!!.coinNumber)
                    withContext(Dispatchers.Main) {
                        if (transferAmount > 0) {
                            val convertAmountToDisplayUnit =
                                convertAmountToDisplayUnit(transferAmount, coinType)
                            editAmountInput.setText(convertAmountToDisplayUnit.first)
                        }
                        if (isToken) {
                            title = "${mAssetsVo.getAssetsName()} ${getString(R.string.transfer)}"
                            tvHintCoinName.text = mAssetsVo.getAssetsName()
                        } else {
                            title = "${coinType.coinName()} ${getString(R.string.transfer)}"
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
                    this@LibraTransferActivity,
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
                getString(R.string.hint_transfer_amount),
                mAssetsVo.amountWithUnit.amount,
                mAssetsVo.amountWithUnit.unit
            )
        }

        mBalance = BigDecimal(mAssetsVo.getAmount())
    }

    private fun checkBalance(): Boolean {
        when (account?.coinNumber) {
            CoinTypes.Libra.coinType(),
            CoinTypes.Violas.coinType() -> {
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

        authenticateAccount(account!!, mAccountManager) {
            launch(Dispatchers.IO) {
                mTransferManager.transfer(
                    this@LibraTransferActivity,
                    address.trim(),
                    amount.trim(),
                    it,
                    account!!,
                    sbQuota.progress,
                    isToken,
                    mAssetsVo.getId(),
                    {
                        showToast(getString(R.string.hint_transfer_broadcast_success))
                        CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
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
    }

    override fun onSelectAddress(address: String) {
        editAddressInput.setText(address)
    }

    override fun onScanAddressQr(address: String) {
        editAddressInput.setText(address)
    }
}
