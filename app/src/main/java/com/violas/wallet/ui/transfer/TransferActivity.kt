package com.violas.wallet.ui.transfer

import android.accounts.AccountsException
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.utils.start
import kotlinx.android.synthetic.main.activity_transfer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

class TransferActivity : BaseActivity() {
    companion object {
        private const val EXT_ACCOUNT_ID = "0"
        private const val EXT_ADDRESS = "1"
        private const val EXT_AMOUNT = "2"
        private const val EXT_IS_TOKEN = "3"
        private const val EXT_TOKEN_ID = "4"

        private const val REQUEST_SELECTOR_ADDRESS = 1

        fun start(
            context: Context,
            accountId: Long,
            address: String = "",
            amount: Long = 0,
            isToken: Boolean = false,
            tokenId: Long = 0
        ) {
            Intent(context, TransferActivity::class.java)
                .apply {
                    putExtra(EXT_ACCOUNT_ID, accountId)
                    putExtra(EXT_ADDRESS, address)
                    putExtra(EXT_AMOUNT, amount)
                    putExtra(EXT_IS_TOKEN, isToken)
                    putExtra(EXT_TOKEN_ID, tokenId)
                }.start(context)
        }
    }

    //BTC
    private lateinit var mTransactionManager: TransactionManager

    private var isToken = false
    private var tokenId = 0L
    private var accountId = 0L
    private var account: AccountDO? = null

    private val mAccountManager by lazy {
        com.violas.wallet.biz.AccountManager()
    }

    private val mTransferManager by lazy {
        com.violas.wallet.biz.TransferManager()
    }

    override fun getLayoutResId() = R.layout.activity_transfer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_transfer)
        accountId = intent.getLongExtra(EXT_ACCOUNT_ID, 0)
        tokenId = intent.getLongExtra(EXT_TOKEN_ID, 0)
        isToken = intent.getBooleanExtra(EXT_IS_TOKEN, false)
        refreshCurrentAmount()
        launch(Dispatchers.IO) {
            try {
                account = mAccountManager.currentAccount()
                account?.apply {
                    if (coinNumber == CoinTypes.Bitcoin.coinType() || coinNumber == CoinTypes.BitcoinTest.coinType()) {
                        mTransactionManager = TransactionManager(arrayListOf(address))
                        mTransactionManager.setFeeCallback {
                            launch {
                                tvFee.text = "$it BTC"
                            }
                        }
                    }
                }

                val amount = BigDecimal(
                    intent
                        .getLongExtra(
                            EXT_AMOUNT,
                            0
                        )
                        .toString()
                )
                    .divide(
                        BigDecimal("${getCoinDecimal(account!!.coinNumber)}"),
                        8,
                        RoundingMode.HALF_DOWN
                    )

                val parseCoinType = CoinTypes.parseCoinType(account!!.coinNumber)
                withContext(Dispatchers.Main) {
                    if (amount > BigDecimal("0")) {
                        editAmountInput.setText(amount.stripTrailingZeros().toPlainString())
                    }
                    title = "${parseCoinType.coinName()}${getString(R.string.transfer)}"
                }
            } catch (e: AccountsException) {
                finish()
            }
        }
        initViewData()

        btnConfirm.setOnClickListener {
            send()
        }
        tvAddressBook.setOnClickListener {
            account?.coinNumber?.let { it1 ->
                AddressBookActivity.start(
                    this@TransferActivity,
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
        mAccountManager.getCurrentBalance() { balance, unit ->
            launch {
                tvCoinAmount.text = String.format(
                    getString(R.string.hint_transfer_amount),
                    balance,
                    unit
                )
            }
        }
    }

    private fun getCoinDecimal(coinNumber: Int): Long {
        return when (coinNumber) {
            CoinTypes.Libra.coinType(),
            CoinTypes.VToken.coinType() -> {
                1000000
            }
            CoinTypes.Bitcoin.coinType(),
            CoinTypes.BitcoinTest.coinType() -> {
                100000000
            }
            else -> {
                1000000
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
        // TODO 密码
        val password = "123123"
        account?.let {
            mTransferManager.transfer(
                this,
                address,
                amount,
                password,
                account!!,
                sbQuota.progress,
                isToken,
                tokenId,
                {
                    print(it)
                },
                {
                    it.printStackTrace()
                })
        }
    }

    private fun initViewData() {
        editAddressInput.setText(intent.getStringExtra(EXT_ADDRESS))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECTOR_ADDRESS && resultCode == Activity.RESULT_OK) {
            data?.apply {
                val address = getStringExtra(AddressBookActivity.RESULT_SELECT_ADDRESS) ?: ""
                editAddressInput.setText(address)
            }
        }
    }
}
