package com.violas.wallet.ui.transactionDetails

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.palliums.base.ViewController
import com.palliums.extensions.close
import com.palliums.extensions.expandTouchArea
import com.palliums.extensions.show
import com.palliums.utils.*
import com.palliums.widget.loading.LoadingDialog
import com.violas.wallet.R
import com.violas.wallet.biz.AddressBookManager
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.common.SYSTEM_ALBUM_DIR_NAME
import com.violas.wallet.ui.addressBook.add.AddAddressBookActivity
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import com.violas.wallet.ui.transactionRecord.TransactionRecordVO
import com.violas.wallet.ui.transactionRecord.TransactionState
import com.violas.wallet.ui.transactionRecord.TransactionType
import com.violas.wallet.ui.web.WebCommonActivity
import com.violas.wallet.utils.ClipboardUtils
import com.violas.wallet.utils.convertAmountToDisplayUnit
import kotlinx.android.synthetic.main.activity_transaction_details.*
import kotlinx.coroutines.*
import me.yokeyword.fragmentation.SupportActivity
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest

/**
 * Created by elephant on 2020/6/8 15:05.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易详情页面
 */
class TransactionDetailsActivity : SupportActivity(), ViewController,
    EasyPermissions.PermissionCallbacks, CoroutineScope by CustomMainScope() {

    companion object {
        private const val REQUEST_CODE_SAVE_PICTURE = 100
        private const val REQUEST_CODE_ADD_ADDRESS = 101

        fun start(context: Context, record: TransactionRecordVO) {
            Intent(context, TransactionDetailsActivity::class.java)
                .apply { putExtra(KEY_ONE, record) }
                .start(context)
        }
    }

    private lateinit var mTransactionRecord: TransactionRecordVO

    private var mLoadingDialog: LoadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setSystemBar(lightModeStatusBar = true, lightModeNavigationBar = true)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_transaction_details)
        if (initData(savedInstanceState)) {
            initEvent()
            initView(mTransactionRecord)
        } else {
            close()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_ONE, mTransactionRecord)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_transaction_details, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.app_bar_share -> {
                var marginTop = clTransactionInfo.y
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                    marginTop -= StatusBarUtil.getStatusBarHeight() + DensityUtility.dp2px(this, 12)

                ShareTransactionDetailsDialog.start(
                    supportFragmentManager, mTransactionRecord, marginTop
                ) {
                    when (it) {
                        0 -> checkStoragePermission()
                    }
                }
            }
        }
        return true
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        var record: TransactionRecordVO? = null
        if (savedInstanceState != null) {
            record = savedInstanceState.getParcelable(KEY_ONE)
        } else if (intent != null) {
            record = intent.getParcelableExtra(KEY_ONE)
        }

        return if (record == null) {
            false
        } else {
            mTransactionRecord = record
            true
        }
    }

    private fun initEvent() {
        tvViewByBrowser.setOnClickListener {
            if (!openBrowser(this, mTransactionRecord.url!!)) {
                WebCommonActivity.start(this, mTransactionRecord.url!!)
            }
        }

        llReceiptAddress.setOnClickListener {
            if (!mTransactionRecord.toAddress.isNullOrBlank()) {
                ClipboardUtils.copy(this, mTransactionRecord.toAddress!!)
            }
        }

        llPaymentAddress.setOnClickListener {
            if (!mTransactionRecord.fromAddress.isBlank()) {
                ClipboardUtils.copy(this, mTransactionRecord.fromAddress)
            }
        }

        llTransactionNumber.setOnClickListener {
            if (!mTransactionRecord.transactionId.isBlank()) {
                ClipboardUtils.copy(this, mTransactionRecord.transactionId)
            }
        }

        tvAddAddress.expandTouchArea()
        tvAddAddress.setOnClickListener {
            AddAddressBookActivity.start(
                this,
                REQUEST_CODE_ADD_ADDRESS,
                mTransactionRecord.coinType.coinNumber(),
                mTransactionRecord.toAddress
            )
        }
    }

    private fun initAddAddressView() {
        if (mTransactionRecord.transactionType != TransactionType.TRANSFER
            || mTransactionRecord.toAddress.isNullOrBlank()
        ) {
            return
        }

        launch {
            val isAdded = withContext(Dispatchers.IO) {
                AddressBookManager().isAddressAdded(
                    mTransactionRecord.coinType.coinNumber(),
                    mTransactionRecord.toAddress!!
                )
            }

            if (!isAdded) {
                tvAddressNotAdd.visibility = View.VISIBLE
                tvAddAddress.visibility = View.VISIBLE
            }
        }
    }

    private fun initView(transactionRecord: TransactionRecordVO) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener {
            onBackPressedSupport()
        }
        toolbar.layoutParams = (toolbar.layoutParams as ConstraintLayout.LayoutParams).apply {
            topMargin = StatusBarUtil.getStatusBarHeight()
        }

        initAddAddressView()

        when (transactionRecord.transactionState) {
            TransactionState.PENDING -> {
                ivState.setImageResource(
                    getResourceId(R.attr.iconRecordStateProcessing, this)
                )
                tvDesc.setTextColor(
                    getColorByAttrId(R.attr.textColorProcessing, this)
                )
                tvDesc.setText(R.string.txn_details_state_processing)
            }

            TransactionState.FAILURE -> {
                ivState.setImageResource(
                    getResourceId(R.attr.iconRecordStateFailed, this)
                )
                tvDesc.setTextColor(
                    getColorByAttrId(R.attr.textColorFailure, this)
                )
                tvDesc.setText(
                    when (transactionRecord.transactionType) {
                        TransactionType.TRANSFER -> {
                            R.string.txn_details_state_transfer_failure
                        }

                        TransactionType.COLLECTION -> {
                            R.string.txn_details_state_collection_failure
                        }

                        TransactionType.ADD_CURRENCY -> {
                            R.string.txn_details_state_add_currency_failure
                        }

                        else -> {
                            R.string.txn_details_state_transaction_failure
                        }
                    }
                )
            }

            else -> {
                ivState.setImageResource(
                    getResourceId(R.attr.iconRecordStateSucceeded, this)
                )
                tvDesc.setTextColor(
                    getColorByAttrId(R.attr.textColorSuccess, this)
                )
                tvDesc.setText(
                    when (transactionRecord.transactionType) {
                        TransactionType.TRANSFER -> {
                            R.string.txn_details_state_transfer_success
                        }

                        TransactionType.COLLECTION -> {
                            R.string.txn_details_state_collection_success
                        }

                        TransactionType.ADD_CURRENCY -> {
                            R.string.txn_details_state_add_currency_success
                        }

                        else -> {
                            R.string.txn_details_state_transaction_success
                        }
                    }
                )
            }
        }

        tvTime.text = formatDateWithNotNeedCorrectDate(
            transactionRecord.time,
            pattern = "yyyy-MM-dd HH:mm:ss"
        )

        val amount = transactionRecord.amount.toBigDecimalOrNull()
        if (amount != null) {
            val amountWithUnit = convertAmountToDisplayUnit(amount, transactionRecord.coinType)
            tvAmount.text =
                "${amountWithUnit.first} ${transactionRecord.tokenDisplayName ?: amountWithUnit.second}"
        } else {
            tvAmount.setText(R.string.common_desc_value_null)
        }

        val gas = transactionRecord.gas.toBigDecimalOrNull()
        if (gas != null) {
            val gasWithUnit = convertAmountToDisplayUnit(gas, transactionRecord.coinType)
            tvGas.text =
                "${gasWithUnit.first} ${transactionRecord.gasTokenDisplayName ?: gasWithUnit.second}"
        } else {
            tvGas.setText(R.string.common_desc_value_null)
        }

        if (transactionRecord.toAddress.isNullOrBlank()) {
            noneContent(tvReceiptAddress)
        } else {
            tvReceiptAddress.text = transactionRecord.toAddress
        }

        if (transactionRecord.fromAddress.isBlank()) {
            noneContent(tvPaymentAddress)
        } else {
            tvPaymentAddress.text = transactionRecord.fromAddress
        }

        if (transactionRecord.transactionId.isBlank()) {
            noneContent(tvTransactionNumber)
        } else {
            tvTransactionNumber.text = transactionRecord.transactionId
        }
    }

    private fun noneContent(textView: TextView) {
        textView.setText(R.string.common_desc_value_null)
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }

    private fun checkStoragePermission() {
        val perms = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (EasyPermissions.hasPermissions(this, *perms)) {
            savePicture()
        } else {
            EasyPermissions.requestPermissions(
                PermissionRequest.Builder(this, REQUEST_CODE_SAVE_PICTURE, *perms)
                    .setRationale(R.string.save_picture_need_permissions_desc)
                    .setNegativeButtonText(R.string.common_action_cancel)
                    .setPositiveButtonText(R.string.common_action_ok)
                    .setTheme(R.style.AppAlertDialog)
                    .build()
            )
        }
    }

    private fun savePicture() {
        launch {
            showProgress()

            val result = withContext(Dispatchers.IO) {
                val bitmap = viewConversionBitmap()
                return@withContext bitmap.saveIntoSystemAlbum(SYSTEM_ALBUM_DIR_NAME)
            }

            delay(300)

            dismissProgress()
            if (result) {
                showToast(R.string.save_picture_tips_success)
            } else {
                showToast(R.string.save_picture_tips_failure)
            }
        }
    }

    private fun viewConversionBitmap(): Bitmap {
        val width = clTransactionInfo.width
        val height = clTransactionInfo.height

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        //canvas.drawColor(Color.WHITE)
        clTransactionInfo.draw(canvas)

        return bitmap
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MultiLanguageUtility.attachBaseContext(newBase))
    }

    override fun showProgress(@StringRes resId: Int) {
        showProgress(getString(resId))
    }

    override fun showProgress(msg: String?) {
        launch {
            if (mLoadingDialog == null) {
                mLoadingDialog = LoadingDialog().setMessage(msg)
                mLoadingDialog!!.show(supportFragmentManager)
            } else {
                mLoadingDialog!!.setMessage(msg)
            }
        }
    }

    override fun dismissProgress() {
        launch {
            mLoadingDialog?.close()
            mLoadingDialog = null
        }
    }

    override fun showToast(@StringRes msgId: Int, duration: Int) {
        showToast(getString(msgId), duration)
    }

    override fun showToast(msg: String, duration: Int) {
        launch {
            Toast.makeText(this@TransactionDetailsActivity, msg, duration).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this)
                .setTitle(getString(R.string.save_picture_set_permissions_title))
                .setRationale(getString(R.string.save_picture_set_permissions_desc))
                .setNegativeButton(R.string.common_action_cancel)
                .setPositiveButton(R.string.common_action_ok)
                .setThemeResId(R.style.AppAlertDialog)
                .build()
                .show()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        when (requestCode) {
            REQUEST_CODE_SAVE_PICTURE -> {
                savePicture()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ADD_ADDRESS && resultCode == Activity.RESULT_OK) {
            tvAddressNotAdd.visibility = View.GONE
            tvAddAddress.visibility = View.GONE
        }
    }
}