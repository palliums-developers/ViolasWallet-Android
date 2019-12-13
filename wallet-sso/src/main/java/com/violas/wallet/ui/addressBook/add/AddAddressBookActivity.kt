package com.violas.wallet.ui.addressBook.add

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.lxj.xpopup.XPopup
import com.palliums.utils.start
import com.palliums.widget.popup.AttachListPopupViewSupport
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AddressBookManager
import com.violas.wallet.biz.decodeScanQRCode
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.database.entity.AddressBookDo
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.utils.validationBTCAddress
import com.violas.wallet.utils.validationLibraAddress
import kotlinx.android.synthetic.main.activity_add_address_book.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddAddressBookActivity : BaseAppActivity() {
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

    private var mCoinTypes = Int.MIN_VALUE

    private val mCoinList by lazy {
        linkedMapOf(
            Pair(Int.MIN_VALUE, getString(R.string.action_please_choose)),
            Pair(CoinTypes.Violas.coinType(), CoinTypes.Violas.fullName()),
            Pair(CoinTypes.Libra.coinType(), CoinTypes.Libra.fullName()),
            if (Vm.TestNet) {
                Pair(CoinTypes.BitcoinTest.coinType(), CoinTypes.BitcoinTest.fullName())
            } else {
                Pair(CoinTypes.Bitcoin.coinType(), CoinTypes.Bitcoin.fullName())
            }
        )
    }
    private val mAddressBookManager by lazy {
        AddressBookManager()
    }

    override fun getLayoutResId() = R.layout.activity_add_address_book

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_add_address_book)

        mCoinTypes = intent.getIntExtra(EXT_COIN_TYPE, Int.MIN_VALUE)
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
            if (mCoinTypes == Int.MIN_VALUE) {
                showToast(getString(R.string.hint_please_select_address_system))
                return@setOnClickListener
            }
            val checkAddress = when (mCoinTypes) {
                CoinTypes.BitcoinTest.coinType(),
                CoinTypes.Bitcoin.coinType() -> {
                    validationBTCAddress(address)
                }
                CoinTypes.Violas.coinType(),
                CoinTypes.Libra.coinType() -> {
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
                        coin_number = mCoinTypes
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
                            setStringData(
                                mCoinList.values.toTypedArray(),
                                null
                            )
                            setOnSelectListener { _, text ->
                                mCoinList.forEach {
                                    if (it.value == text) {
                                        mCoinTypes = it.key
                                    }
                                }
                                refreshCoinType()
                            }
                        }
                )
                .show()
        }
    }

    private fun refreshCoinType() {
        editCoinType.text = mCoinList[mCoinTypes]
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SCAN_QR_CODE -> {
                data?.getStringExtra(ScanActivity.RESULT_QR_CODE_DATA)?.let { msg ->
                    decodeScanQRCode(msg) { coinType, address, _, _ ->
                        launch {
                            if (coinType == Int.MIN_VALUE) {
                                editAddress.setText(address)
                            } else {
                                editAddress.setText(address)
                                try {
                                    mCoinTypes = CoinTypes.parseCoinType(coinType).coinType()
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
