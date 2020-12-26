package com.violas.wallet.ui.addressBook.add

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.*
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.database.entity.AddressBookDo
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.utils.validationBTCAddress
import com.violas.wallet.utils.validationLibraAddress
import com.violas.wallet.utils.validationViolasAddress
import kotlinx.android.synthetic.main.activity_add_address_book.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddAddressBookActivity : BaseAppActivity() {
    companion object {
        private const val REQUEST_SCAN_QR_CODE = 1

        private const val EXT_COIN_TYPE = "a1"
        private const val EXT_ADDRESS = "a2"

        fun start(
            context: Activity,
            requestCode: Int,
            coinType: Int = CoinTypes.Bitcoin.coinType(),
            address: String? = null
        ) {
            Intent(context, AddAddressBookActivity::class.java).apply {
                putExtra(EXT_COIN_TYPE, coinType)
                if (!address.isNullOrBlank()) {
                    putExtra(EXT_ADDRESS, address)
                }
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

        val address = intent.getStringExtra(EXT_ADDRESS)
        if (!address.isNullOrBlank()) {
            editAddress.setText(address)
        }

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
                CoinTypes.Violas.coinType() -> {
                    validationViolasAddress(address)
                }
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

        coinTypeGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.coinTypeLibra -> {
                    mCoinTypes = CoinTypes.Libra.coinType()
                }
                R.id.coinTypeViolas -> {
                    mCoinTypes = CoinTypes.Violas.coinType()
                }
                R.id.coinTypeBitcoin -> {
                    mCoinTypes = if (Vm.TestNet) {
                        CoinTypes.BitcoinTest.coinType()
                    } else {
                        CoinTypes.Bitcoin.coinType()
                    }
                }
            }
        }
    }

    private fun refreshCoinType() {
        coinTypeGroup
        when (mCoinTypes) {
            CoinTypes.Violas.coinType() -> {
                coinTypeViolas.isChecked = true
            }
            CoinTypes.Libra.coinType() -> {
                coinTypeLibra.isChecked = true
            }
            CoinTypes.BitcoinTest.coinType(),
            CoinTypes.Bitcoin.coinType() -> {
                coinTypeBitcoin.isChecked = true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SCAN_QR_CODE -> {
                data?.getParcelableExtra<QRCode>(ScanActivity.RESULT_QR_CODE_DATA)?.let { qrCode ->
                    when (qrCode) {
                        is TransferQRCode -> {
                            editAddress.setText(qrCode.address)
                            try {
                                editAddress.setText(qrCode.address)
                                mCoinTypes = CoinTypes.parseCoinType(qrCode.coinType).coinType()
                                refreshCoinType()
                            } catch (e: Exception) {
                            }
                        }

                        is CommonQRCode -> {
                            editAddress.setText(qrCode.content)
                        }
                    }
                }
            }
        }
    }
}
