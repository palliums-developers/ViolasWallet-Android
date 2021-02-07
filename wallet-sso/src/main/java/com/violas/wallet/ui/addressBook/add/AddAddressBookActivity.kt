package com.violas.wallet.ui.addressBook.add

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.start
import com.quincysx.crypto.CoinType
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AddressBookManager
import com.violas.wallet.biz.ScanCodeType
import com.violas.wallet.biz.ScanTranBean
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
            coinType: Int = CoinType.Bitcoin.coinNumber()
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
            Pair(CoinType.Violas.coinNumber(), CoinType.Violas.chainName()),
            Pair(CoinType.Diem.coinNumber(), CoinType.Diem.chainName()),
            if (Vm.TestNet) {
                Pair(CoinType.BitcoinTest.coinNumber(), CoinType.BitcoinTest.chainName())
            } else {
                Pair(CoinType.Bitcoin.coinNumber(), CoinType.Bitcoin.chainName())
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
                CoinType.BitcoinTest.coinNumber(),
                CoinType.Bitcoin.coinNumber() -> {
                    validationBTCAddress(address)
                }
                CoinType.Violas.coinNumber(),
                CoinType.Diem.coinNumber() -> {
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

        coinTypeGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.coinTypeLibra -> {
                    mCoinTypes = CoinType.Diem.coinNumber()
                }
                R.id.coinTypeViolas -> {
                    mCoinTypes = CoinType.Violas.coinNumber()
                }
                R.id.coinTypeBitcoin -> {
                    mCoinTypes = if (Vm.TestNet) {
                        CoinType.BitcoinTest.coinNumber()
                    } else {
                        CoinType.Bitcoin.coinNumber()
                    }
                }
            }
        }
    }

    private fun refreshCoinType() {
        when (mCoinTypes) {
            CoinType.Violas.coinNumber() -> {
                coinTypeViolas.isChecked = true
            }
            CoinType.Diem.coinNumber() -> {
                coinTypeLibra.isChecked = true
            }
            CoinType.BitcoinTest.coinNumber(),
            CoinType.Bitcoin.coinNumber() -> {
                coinTypeBitcoin.isChecked = true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SCAN_QR_CODE -> {
                data?.getStringExtra(ScanActivity.RESULT_QR_CODE_DATA)?.let { msg ->
                    decodeScanQRCode(msg) { scanType, scanBean ->
                        launch {
                            when (scanType) {
                                ScanCodeType.Address -> {
                                    scanBean as ScanTranBean
                                    try {
                                        editAddress.setText(scanBean.address)
                                        mCoinTypes =
                                            CoinType.parseCoinNumber(scanBean.coinType).coinNumber()
                                        refreshCoinType()
                                    } catch (e: Exception) {
                                    }
                                }
                                ScanCodeType.Text -> {
                                    editAddress.setText(scanBean.msg)
                                }
                                else -> {
                                    showToast(getString(R.string.hint_address_error))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
