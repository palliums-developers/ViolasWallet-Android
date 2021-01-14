package com.violas.wallet.ui.transfer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.*
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsVo

abstract class TransferActivity : BaseAppActivity() {
    companion object {
        private const val EXT_ACCOUNT_ID = "0"
        private const val EXT_ADDRESS = "1"
        private const val EXT_AMOUNT = "2"
        private const val EXT_IS_TOKEN = "3"
        private const val EXT_ASSETS_NAME = "4"
        private const val EXT_COIN_NUMBER = "5"
        private const val EXT_SUB_ADDRESS = "6"

        const val REQUEST_SELECTOR_ADDRESS = 1
        const val REQUEST_SCAN_QR_CODE = 2

        fun start(
            context: Context,
            assetsVo: AssetsVo,
            address: String? = null,
            subAddress: String? = null,
            amount: Long? = null
        ) {
            val assetsName = if (assetsVo is AssetsCoinVo) {
                null
            } else {
                assetsVo.getAssetsName()
            }
            start(context, assetsVo.getCoinNumber(), address, subAddress, amount, assetsName)
        }

        fun start(
            context: Context,
            coinType: Int,
            address: String? = null,
            subAddress: String? = null,
            amount: Long? = null,
            tokenName: String? = null
        ) {
            var isToken = tokenName == null
            when (coinType) {
                CoinTypes.BitcoinTest.coinType(),
                CoinTypes.Bitcoin.coinType() -> {
                    Intent(context, BTCTransferActivity::class.java)
                }
                else -> {
                    isToken = true
                    Intent(context, LibraTransferActivity::class.java)
                }
            }.apply {
                putExtra(EXT_ADDRESS, address)
                putExtra(EXT_AMOUNT, amount)
                putExtra(EXT_IS_TOKEN, isToken)
                putExtra(EXT_ASSETS_NAME, tokenName)
                putExtra(EXT_COIN_NUMBER, coinType)
                putExtra(EXT_SUB_ADDRESS, subAddress)
            }.start(context)
        }
    }

    var isToken = false
    var assetsName: String? = ""
    var coinNumber: Int = CoinTypes.Violas.coinType()
    var transferAmount = 0L
    var toAddress: String? = ""
    var toSubAddress: String? = ""
    var account: AccountDO? = null

    val mAccountManager by lazy {
        AccountManager()
    }

    val mTransferManager by lazy {
        TransferManager()
    }

    abstract fun onSelectAddress(address: String)
    abstract fun onScanAddressQr(address: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = getString(R.string.title_transfer)
        if (savedInstanceState != null) {
            isToken = savedInstanceState.getBoolean(EXT_IS_TOKEN, false)
            assetsName = savedInstanceState.getString(EXT_ASSETS_NAME)
            coinNumber = savedInstanceState.getInt(EXT_COIN_NUMBER, CoinTypes.Violas.coinType())
            transferAmount = savedInstanceState.getLong(EXT_AMOUNT, 0)
            toAddress = savedInstanceState.getString(EXT_ADDRESS)
            toSubAddress = savedInstanceState.getString(EXT_SUB_ADDRESS)
        } else if (intent != null) {
            isToken = intent.getBooleanExtra(EXT_IS_TOKEN, false)
            assetsName = intent.getStringExtra(EXT_ASSETS_NAME)
            coinNumber = intent.getIntExtra(EXT_COIN_NUMBER, CoinTypes.Violas.coinType())
            transferAmount = intent.getLongExtra(EXT_AMOUNT, 0)
            toAddress = intent.getStringExtra(EXT_ADDRESS)
            toSubAddress = intent.getStringExtra(EXT_SUB_ADDRESS)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXT_IS_TOKEN, isToken)
        assetsName?.let { outState.putString(EXT_ASSETS_NAME, it) }
        outState.putInt(EXT_COIN_NUMBER, coinNumber)
        outState.putLong(EXT_AMOUNT, transferAmount)
        toAddress?.let { outState.putString(EXT_ADDRESS, it) }
        toSubAddress?.let { outState.putString(EXT_SUB_ADDRESS, it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SELECTOR_ADDRESS -> {
                data?.apply {
                    toAddress = getStringExtra(AddressBookActivity.RESULT_SELECT_ADDRESS) ?: ""
                    toSubAddress = null
                    onSelectAddress(toAddress ?: "")
                }
            }
            REQUEST_SCAN_QR_CODE -> {
                data?.getParcelableExtra<QRCode>(ScanActivity.RESULT_QR_CODE_DATA)?.let { qrCode ->
                    when (qrCode) {
                        is TransferQRCode -> {
                            toAddress = qrCode.address
                            toSubAddress = qrCode.subAddress
                            onScanAddressQr(toAddress ?: "")
                        }

                        is CommonQRCode -> {
                            toAddress = qrCode.content
                            toSubAddress = null
                            onScanAddressQr(toAddress ?: "")
                        }
                    }
                }
            }
        }
    }
}