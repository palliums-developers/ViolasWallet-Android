package com.violas.wallet.ui.transfer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.CommonQRCode
import com.violas.wallet.biz.QRCode
import com.violas.wallet.biz.TransferManager
import com.violas.wallet.biz.TransferQRCode
import com.violas.wallet.biz.bean.DiemCurrency
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.viewModel.bean.AssetVo
import com.violas.wallet.viewModel.bean.DiemCurrencyAssetVo

abstract class TransferActivity : BaseAppActivity() {

    companion object {
        private const val EXT_ACCOUNT_ID = "0"
        private const val EXT_COIN_NUMBER = "1"
        private const val EXT_CURRENCY = "2"
        private const val EXT_PAYEE_ADDRESS = "3"
        private const val EXT_PAYEE_SUB_ADDRESS = "4"
        private const val EXT_AMOUNT = "5"

        const val REQUEST_SELECTOR_ADDRESS = 1
        const val REQUEST_SCAN_QR_CODE = 2

        fun start(
            context: Context,
            asset: AssetVo,
            payeeAddress: String? = null,
            payeeSubAddress: String? = null,
            amount: Long? = null
        ) {
            start(
                context,
                asset.getCoinNumber(),
                if (asset is DiemCurrencyAssetVo)
                    asset.currency
                else
                    null,
                payeeAddress,
                payeeSubAddress,
                amount
            )
        }

        fun start(
            context: Context,
            coinNumber: Int,
            currencyCode: String?,
            payeeAddress: String? = null,
            payeeSubAddress: String? = null,
            amount: Long? = null
        ) {

            start(
                context,
                coinNumber,
                if (currencyCode.isNullOrBlank())
                    null
                else
                    DiemCurrency(currencyCode),
                payeeAddress,
                payeeSubAddress,
                amount
            )
        }

        fun start(
            context: Context,
            coinNumber: Int,
            currency: DiemCurrency?,
            payeeAddress: String? = null,
            payeeSubAddress: String? = null,
            amount: Long? = null
        ) {
            when (coinNumber) {
                getBitcoinCoinType().coinNumber() -> {
                    Intent(context, BTCTransferActivity::class.java)
                }
                getDiemCoinType().coinNumber(), getViolasCoinType().coinNumber() -> {
                    Intent(context, DiemTransferActivity::class.java)
                }
                else -> {
                    null
                }
            }?.apply {
                putExtra(EXT_COIN_NUMBER, coinNumber)
                putExtra(EXT_CURRENCY, currency)
                putExtra(EXT_PAYEE_ADDRESS, payeeAddress)
                putExtra(EXT_PAYEE_SUB_ADDRESS, payeeSubAddress)
                putExtra(EXT_AMOUNT, amount)
            }?.start(context)
        }
    }

    var isToken = false
    var mCoinNumber: Int = Int.MIN_VALUE
    var mCurrency: DiemCurrency? = null
    var mPayeeAddress: String? = null
    var mPayeeSubAddress: String? = null
    var mAmount: Long = 0

    var mPayerAccount: AccountDO? = null
    val mTransferManager by lazy { TransferManager() }

    abstract fun onSelectAddress(address: String)
    abstract fun onScanAddressQr(address: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.transfer_title)
        if (savedInstanceState != null) {
            mCoinNumber = savedInstanceState.getInt(EXT_COIN_NUMBER, Int.MIN_VALUE)
            mCurrency = savedInstanceState.getParcelable(EXT_CURRENCY)
            mPayeeAddress = savedInstanceState.getString(EXT_PAYEE_ADDRESS)
            mPayeeSubAddress = savedInstanceState.getString(EXT_PAYEE_SUB_ADDRESS)
            mAmount = savedInstanceState.getLong(EXT_AMOUNT, 0)
        } else if (intent != null) {
            mCoinNumber = intent.getIntExtra(EXT_COIN_NUMBER, Int.MIN_VALUE)
            mCurrency = intent.getParcelableExtra(EXT_CURRENCY)
            mPayeeAddress = intent.getStringExtra(EXT_PAYEE_ADDRESS)
            mPayeeSubAddress = intent.getStringExtra(EXT_PAYEE_SUB_ADDRESS)
            mAmount = intent.getLongExtra(EXT_AMOUNT, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mCoinNumber != Int.MIN_VALUE)
            outState.putInt(EXT_COIN_NUMBER, mCoinNumber)
        mCurrency?.let { outState.putParcelable(EXT_CURRENCY, it) }
        mPayeeAddress?.let { outState.putString(EXT_PAYEE_ADDRESS, it) }
        mPayeeSubAddress?.let { outState.putString(EXT_PAYEE_SUB_ADDRESS, it) }
        outState.putLong(EXT_AMOUNT, mAmount)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SELECTOR_ADDRESS -> {
                data?.getStringExtra(AddressBookActivity.RESULT_SELECT_ADDRESS)?.let {
                    if (it.isBlank()) return@let
                    mPayeeAddress = it
                    mPayeeSubAddress = null
                    onSelectAddress(it)
                }
            }

            REQUEST_SCAN_QR_CODE -> {
                data?.getParcelableExtra<QRCode>(ScanActivity.RESULT_QR_CODE_DATA)?.let { qrCode ->
                    when (qrCode) {
                        is TransferQRCode -> {
                            if (qrCode.address.isBlank()) return@let
                            mPayeeAddress = qrCode.address
                            mPayeeSubAddress = qrCode.subAddress
                            onScanAddressQr(qrCode.address)
                        }

                        is CommonQRCode -> {
                            if (qrCode.content.isBlank()) return@let
                            mPayeeAddress = qrCode.content
                            mPayeeSubAddress = null
                            onScanAddressQr(qrCode.content)
                        }
                    }
                }
            }
        }
    }
}