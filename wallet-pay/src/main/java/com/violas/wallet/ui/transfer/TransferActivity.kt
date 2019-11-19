package com.violas.wallet.ui.transfer

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.WorkerThread
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.*
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.scan.ScanActivity
import kotlinx.coroutines.launch

abstract class TransferActivity : BaseAppActivity() {
    companion object {
        const val EXT_ACCOUNT_ID = "0"
        const val EXT_ADDRESS = "1"
        const val EXT_AMOUNT = "2"
        const val EXT_IS_TOKEN = "3"
        const val EXT_TOKEN_ID = "4"

        const val REQUEST_SELECTOR_ADDRESS = 1
        const val REQUEST_SCAN_QR_CODE = 2

        private val mHandler = Handler(Looper.getMainLooper())

        private fun showToast(context: Context, msg: String) {
            mHandler.post {
                Toast.makeText(context.applicationContext, msg, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        fun start(
            context: Context,
            coinType: Int,
            address: String,
            amount: Long,
            tokenName: String?
        ) {
            val accountManager = AccountManager()
            val tokenManager = TokenManager()

            val currentAccount = accountManager.currentAccount()
            if (coinType == currentAccount.coinNumber) {
                // YES 当前币种是当前需要支付的币种
                if (tokenName != null) {
                    val tokenDo =
                        tokenManager.findTokenByName(currentAccount.id, tokenName)
                    if (tokenDo == null) {
                        // 不支持的币种
                        showToast(context, context.getString(R.string.hint_unsupported_tokens))
                        return
                    } else {
                        start(
                            context,
                            currentAccount.id,
                            address,
                            amount,
                            true,
                            tokenDo.id!!
                        )
                    }
                } else {
                    start(
                        context,
                        currentAccount.id,
                        address,
                        amount
                    )
                }
            } else {
                // NO 当前币种不是当前需要支付的币种
                val account = accountManager.getIdentityByCoinType(coinType)
                if (account == null) {
                    showToast(context, context.getString(R.string.hint_unsupported_coin))
                    return
                }
                if (tokenName != null) {
                    val tokenDo =
                        tokenManager.findTokenByName(account.id, tokenName)
                    if (tokenDo == null) {
                        // 不支持的币种
                        showToast(context, context.getString(R.string.hint_unsupported_tokens))
                        return
                    } else {
                        start(
                            context,
                            account.id,
                            address,
                            amount,
                            true,
                            tokenDo.id!!
                        )
                    }
                } else {
                    start(
                        context,
                        account.id,
                        address,
                        amount
                    )
                }
            }
        }

        @WorkerThread
        fun start(
            context: Context,
            accountId: Long,
            address: String = "",
            amount: Long = 0,
            isToken: Boolean = false,
            tokenId: Long = 0
        ) {
            val account = AccountManager().getAccountById(accountId)
            when (account.coinNumber) {
                CoinTypes.BitcoinTest.coinType(),
                CoinTypes.Bitcoin.coinType() -> {
                    Intent(context, BTCTransferActivity::class.java)
                }
                else -> {
                    Intent(context, LibraTransferActivity::class.java)
                }
            }.apply {
                putExtra(EXT_ACCOUNT_ID, accountId)
                putExtra(EXT_ADDRESS, address)
                putExtra(EXT_AMOUNT, amount)
                putExtra(EXT_IS_TOKEN, isToken)
                putExtra(EXT_TOKEN_ID, tokenId)
            }.start(context)
        }
    }

    var isToken = false
    var tokenId = 0L
    var accountId = 0L
    var account: AccountDO? = null

    val mAccountManager by lazy {
        AccountManager()
    }

    val mTransferManager by lazy {
        TransferManager()
    }

    abstract fun onSelectAddress(address: String)
    abstract fun onScanAddressQr(address: String)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SELECTOR_ADDRESS -> {
                data?.apply {
                    val address = getStringExtra(AddressBookActivity.RESULT_SELECT_ADDRESS) ?: ""
                    onSelectAddress(address)
                }
            }
            REQUEST_SCAN_QR_CODE -> {
                data?.getStringExtra(ScanActivity.RESULT_QR_CODE_DATA)?.let { msg ->
                    decodeScanQRCode(msg) { scanType, scanBean ->
                        launch {
                            account?.let {
                                when (scanType) {
                                    ScanCodeType.Address -> {
                                        scanBean as ScanTranBean
                                        onScanAddressQr(scanBean.address)
                                    }
                                    ScanCodeType.Text -> {
                                        onScanAddressQr(scanBean.msg)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}