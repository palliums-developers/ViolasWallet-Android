package com.violas.wallet.ui.addressBook.add

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.lxj.xpopup.XPopup
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.base.dialog.AttachListPopupViewSupport
import com.violas.wallet.biz.AddressBookManager
import com.violas.wallet.biz.decodeScanQRCode
import com.violas.wallet.repository.database.entity.AddressBookDo
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.utils.start
import com.violas.wallet.utils.validationBTCAddress
import com.violas.wallet.utils.validationLibraAddress
import kotlinx.android.synthetic.main.activity_add_address_book.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddAddressBookActivity : BaseActivity() {
    companion object {
        private const val REQUEST_SCAN_QR_CODE = 1

        private const val EXT_COIN_TYPE = "a1"
        fun start(
            context: Activity,
            requestCode: Int,
            coinType: Int = CoinTypes.Bitcoin.coinType()
        ) {
            Intent(context, AddAddressBookActivity::class.java).apply {
                putExtra(EXT_COIN_TYPE, coinType)
            }.start(context, requestCode)
        }
    }

    private var mCoinTypes = CoinTypes.Bitcoin
    private var mCoinList = mapOf(
        Pair(CoinTypes.VToken.coinName(), CoinTypes.VToken),
        Pair(CoinTypes.Libra.coinName(), CoinTypes.Libra),
        Pair(CoinTypes.Bitcoin.coinName(), CoinTypes.Bitcoin),
        Pair(CoinTypes.BitcoinTest.coinName(), CoinTypes.BitcoinTest)
    )
    private val mAddressBookManager by lazy {
        AddressBookManager()
    }

    override fun getLayoutResId() = R.layout.activity_add_address_book

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_add_address_book)

        mCoinTypes =
            CoinTypes.parseCoinType(intent.getIntExtra(EXT_COIN_TYPE, CoinTypes.Bitcoin.coinType()))
        refreshCoinType()
        btnScan.setOnClickListener {
            ScanActivity.start(this@AddAddressBookActivity, REQUEST_SCAN_QR_CODE)
        }
        btnAdd.setOnClickListener {
            val note = editNote.text.toString().trim()
            if (note.isEmpty()) {
                showToast(getString(R.string.hint_input_note))
                return@setOnClickListener
            }
            val address = editAddress.text.toString().trim()
            if (address.isEmpty()) {
                showToast(getString(R.string.hint_input_address))
                return@setOnClickListener
            }
            val checkAddress = when (mCoinTypes) {
                CoinTypes.BitcoinTest,
                CoinTypes.Bitcoin -> {
                    validationBTCAddress(address)
                }
                CoinTypes.VToken,
                CoinTypes.Libra -> {
                    validationLibraAddress(address)
                }
                else -> {
                    false
                }
            }
            if (!checkAddress) {
                showToast(getString(R.string.hint_input_address_error))
                return@setOnClickListener
            }
            launch(Dispatchers.IO) {
                mAddressBookManager.install(
                    AddressBookDo(
                        note = note,
                        address = address,
                        coin_number = mCoinTypes.coinType()
                    )
                )
                showToast(getString(R.string.hint_address_success))
                withContext(Dispatchers.Main) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }

        editCoinType.setOnClickListener {
            XPopup.Builder(this)
                .hasShadowBg(false)
                .atView(editCoinType)  // 依附于所点击的View，内部会自动判断在上方或者下方显示
                .setPopupCallback(object : AttachListPopupViewSupport.PopupCallbackSupport() {

                    override fun onShowBefore() {
                        editCoinType.isSelected = !editCoinType.isSelected
                    }

                    override fun onDismissBefore() {
                        editCoinType.isSelected = !editCoinType.isSelected
                    }
                })
                .asCustom(
                    AttachListPopupViewSupport(this)
                        .apply {
                            setStringData(mCoinList.keys.toTypedArray(), null)
                            setOnSelectListener { _, text ->
                                mCoinList[text]?.also {
                                    mCoinTypes = it
                                    refreshCoinType()
                                }
                            }
                        }
                )
                .show()
        }
    }

    private fun refreshCoinType() {
        editCoinType.text = mCoinTypes.coinName()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SCAN_QR_CODE -> {
                data?.getStringExtra(ScanActivity.RESULT_QR_CODE_DATA)?.let { msg ->
                    decodeScanQRCode(msg) { coinType, address, amount ->
                        launch {
                            if (coinType == -1) {
                                editAddress.setText(address)
                            } else {
                                editAddress.setText(address)
                                try {
                                    mCoinTypes = CoinTypes.parseCoinType(coinType)
                                    refreshCoinType()
                                } catch (e: Exception) {
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
