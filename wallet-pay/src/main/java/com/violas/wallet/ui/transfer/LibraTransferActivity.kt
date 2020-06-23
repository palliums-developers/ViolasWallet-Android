package com.violas.wallet.ui.transfer

import android.accounts.AccountsException
import android.os.Bundle
import android.text.AmountInputFilter
import android.util.Log
import androidx.lifecycle.Observer
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.event.RefreshBalanceEvent
import com.violas.wallet.repository.database.entity.TokenDo
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.utils.convertViolasTokenUnit
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.android.synthetic.main.activity_transfer.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import java.math.BigDecimal

class LibraTransferActivity : TransferActivity() {
    override fun getLayoutResId() = R.layout.activity_transfer

    private var mBalance: BigDecimal? = null
    private lateinit var mAssetsVo: AssetsVo

    private val mWalletAppViewModel by lazy {
        WalletAppViewModel.getViewModelInstance(this@LibraTransferActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_transfer)
        assetsName = intent.getStringExtra(EXT_ASSETS_NAME)
        coinNumber = intent.getIntExtra(EXT_COIN_NUMBER, CoinTypes.Violas.coinType())
        isToken = intent.getBooleanExtra(EXT_IS_TOKEN, false)

        mWalletAppViewModel.refreshAssetsList()
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

    private fun initViewData() {
        editAddressInput.setText(intent.getStringExtra(EXT_ADDRESS))
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
                        EventBus.getDefault().post(RefreshBalanceEvent())
                        GlobalScope.launch {
                            // todo 刷新单币种价格
                            // todo 优化成命令模式
                            delay(3000)
                            mWalletAppViewModel.refreshAssetsList()
                        }
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
