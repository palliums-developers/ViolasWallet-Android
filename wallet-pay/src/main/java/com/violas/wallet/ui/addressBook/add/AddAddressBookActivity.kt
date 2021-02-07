package com.violas.wallet.ui.addressBook.add

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.start
import com.quincysx.crypto.CoinType
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.*
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.database.entity.AddressBookDo
import com.violas.wallet.ui.scan.ScanActivity
import com.violas.wallet.utils.*
import kotlinx.android.synthetic.main.activity_add_address_book.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 添加地址页面
 */
class AddAddressBookActivity : BaseAppActivity() {

    companion object {
        private const val REQUEST_SCAN_QR_CODE = 1

        private const val EXT_COIN_TYPE = "a1"
        private const val EXT_ADDRESS = "a2"

        fun start(
            context: Activity,
            requestCode: Int,
            coinType: Int = getBitcoinCoinType().coinNumber(),
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

    private val mAddressBookManager by lazy {
        AddressBookManager()
    }

    override fun getLayoutResId() = R.layout.activity_add_address_book

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.add_address_title)

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
                showToast(getString(R.string.add_address_tips_note_empty))
                return@setOnClickListener
            }

            val address = editAddress.text.toString().trim()
            if (address.isEmpty()) {
                showToast(getString(R.string.add_address_tips_address_empty))
                return@setOnClickListener
            }

            if (mCoinTypes == Int.MIN_VALUE) {
                showToast(getString(R.string.add_address_tips_type_empty))
                return@setOnClickListener
            }

            val checkAddress = when (mCoinTypes) {
                getBitcoinCoinType().coinNumber() -> {
                    validationBTCAddress(address)
                }
                getViolasCoinType().coinNumber() -> {
                    validationViolasAddress(address)
                }
                getDiemCoinType().coinNumber() -> {
                    validationLibraAddress(address)
                }
                else -> {
                    false
                }
            }
            if (!checkAddress) {
                showToast(getString(R.string.add_address_tips_address_error))
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
                showToast(getString(R.string.add_address_tips_add_success))
                withContext(Dispatchers.Main) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }

        coinTypeGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.coinTypeLibra -> {
                    mCoinTypes = getDiemCoinType().coinNumber()
                }
                R.id.coinTypeViolas -> {
                    mCoinTypes = getViolasCoinType().coinNumber()
                }
                R.id.coinTypeBitcoin -> {
                    mCoinTypes = getBitcoinCoinType().coinNumber()
                }
            }
        }
    }

    private fun refreshCoinType() {
        coinTypeGroup
        when (mCoinTypes) {
            getViolasCoinType().coinNumber() -> {
                coinTypeViolas.isChecked = true
            }
            getDiemCoinType().coinNumber() -> {
                coinTypeLibra.isChecked = true
            }
            getBitcoinCoinType().coinNumber() -> {
                coinTypeBitcoin.isChecked = true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SCAN_QR_CODE -> {
                data?.getParcelableExtra<QRCode>(ScanActivity.RESULT_QR_CODE_DATA)
                    ?.let { qrCode ->
                        when (qrCode) {
                            is TransferQRCode -> {
                                try {
                                    editAddress.setText(qrCode.address)
                                    mCoinTypes =
                                        CoinType.parseCoinNumber(qrCode.coinType).coinNumber()
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
