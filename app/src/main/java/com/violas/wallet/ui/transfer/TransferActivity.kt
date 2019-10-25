package com.violas.wallet.ui.transfer

import android.accounts.AccountsException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.repository.database.entity.AccountDO
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
        title = "转账"
        accountId = intent.getLongExtra(EXT_ACCOUNT_ID, 0)
        tokenId = intent.getLongExtra(EXT_TOKEN_ID, 0)
        isToken = intent.getBooleanExtra(EXT_IS_TOKEN, false)
        launch(Dispatchers.IO) {
            try {
                account = mAccountManager.currentAccount()
                val amount = BigDecimal(
                    intent.getLongExtra(
                        EXT_AMOUNT,
                        0
                    ).toString()
                )
                    .divide(
                        BigDecimal("${getCoinDecimal(account!!.coinNumber)}"),
                        8,
                        RoundingMode.HALF_DOWN
                    )
                    .stripTrailingZeros()
                    .toPlainString()

                val parseCoinType = CoinTypes.parseCoinType(account!!.coinNumber)
                withContext(Dispatchers.Main) {
                    editAmountInput.setText(amount)
                    title = "${parseCoinType.coinName()}转账"
                }
            } catch (e: AccountsException) {
                finish()
            }
        }
        initViewData()

        btnConfirm.setOnClickListener {
            send()
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
            showToast("请填写转账金额")
            return
        }
        if (address.isEmpty()) {
            showToast("请填写转账地址")
            return
        }
        val password = "123123"
        account?.let {
            mTransferManager.transfer(
                this,
                address,
                amount,
                password,
                account!!,
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
}
