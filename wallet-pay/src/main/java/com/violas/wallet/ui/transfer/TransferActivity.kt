package com.violas.wallet.ui.transfer

import android.content.Context
import android.content.Intent
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.*
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.coroutines.launch

abstract class TransferActivity : BaseAppActivity() {
    companion object {
        const val EXT_ACCOUNT_ID = "0"
        const val EXT_ADDRESS = "1"
        const val EXT_AMOUNT = "2"
        const val EXT_IS_TOKEN = "3"
        const val EXT_ASSETS_NAME = "4"
        const val EXT_COIN_NUMBER = "5"

        const val REQUEST_SELECTOR_ADDRESS = 1
        const val REQUEST_SCAN_QR_CODE = 2

        fun start(
            context: Context,
            assetsVo: AssetsVo,
            toAddress: String? = null,
            amount: Long? = null
        ) {
            val assetsName = if (assetsVo is AssetsCoinVo) {
                null
            } else {
                assetsVo.getAssetsName()
            }
            start(context, assetsVo.getCoinNumber(), toAddress, amount, assetsName)
        }

        fun start(
            context: Context,
            coinType: Int,
            address: String? = null,
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
            }.start(context)
        }
    }

    var isToken = false
    var assetsName: String? = ""
    var account: AccountDO? = null
    var coinNumber: Int = CoinTypes.Violas.coinType()

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
                                if (scanType == ScanCodeType.Address) {
                                    scanBean as ScanTranBean
                                    onScanAddressQr(scanBean.address)
                                } else if (scanType == ScanCodeType.Text) {
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