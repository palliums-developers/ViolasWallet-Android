package com.violas.wallet.ui.transactionDetails

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.palliums.base.ViewController
import com.palliums.extensions.close
import com.palliums.extensions.show
import com.palliums.utils.*
import com.palliums.widget.loading.LoadingDialog
import com.violas.wallet.R
import com.violas.wallet.common.KEY_ONE
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
import java.io.File
import java.io.OutputStream

/**
 * Created by elephant on 2020/6/8 15:05.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易详情页面
 */
class TransactionDetailsActivity : SupportActivity(), ViewController,
    EasyPermissions.PermissionCallbacks, CoroutineScope by CustomMainScope() {

    companion object {
        private const val PIC_DIR_NAME = "ViolasPay Photos"
        private const val REQUEST_CODE_SAVE_PICTURE = 100

        fun start(context: Context, record: TransactionRecordVO) {
            Intent(context, TransactionDetailsActivity::class.java)
                .apply { putExtra(KEY_ONE, record) }
                .start(context)
        }
    }

    private lateinit var mTransactionRecord: TransactionRecordVO

    private var mLoadingDialog: LoadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        StatusBarUtil.layoutExtendsToStatusBar(window)
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
                ShareTransactionDetailsDialog.start(
                    supportFragmentManager, mTransactionRecord, clTransactionInfo.y
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
    }

    private fun initView(transactionRecord: TransactionRecordVO) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener {
            onBackPressedSupport()
        }
        toolbar.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val rectangle = Rect()
                val window = window
                window.decorView.getWindowVisibleDisplayFrame(rectangle)
                val statusBarHeight = rectangle.top

                val toolbarLayoutParams =
                    toolbar.layoutParams as ConstraintLayout.LayoutParams
                toolbarLayoutParams.height = toolbarLayoutParams.height + statusBarHeight
                toolbar.layoutParams = toolbarLayoutParams
                toolbar.setPadding(0, statusBarHeight, 0, 0)

                toolbar.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        when (transactionRecord.transactionState) {
            TransactionState.PENDING -> {
                ivState.setImageResource(
                    getResourceId(R.attr.transDetailsProcessingIcon, this)
                )
                tvDesc.setTextColor(
                    getColorByAttrId(R.attr.textColorProcessing, this)
                )
                tvDesc.setText(R.string.desc_transaction_state_transaction_pending)
            }

            TransactionState.FAILURE -> {
                ivState.setImageResource(
                    getResourceId(R.attr.transDetailsFailureIcon, this)
                )
                tvDesc.setTextColor(
                    getColorByAttrId(R.attr.textColorFailure, this)
                )
                tvDesc.setText(
                    when (transactionRecord.transactionType) {
                        TransactionType.TRANSFER -> {
                            R.string.desc_transaction_state_transfer_failure
                        }

                        TransactionType.COLLECTION -> {
                            R.string.desc_transaction_state_collection_failure
                        }

                        TransactionType.REGISTER -> {
                            R.string.desc_transaction_state_register_failure
                        }

                        else -> {
                            R.string.desc_transaction_state_transaction_failure
                        }
                    }
                )
            }

            else -> {
                ivState.setImageResource(
                    getResourceId(R.attr.transDetailsSuccessIcon, this)
                )
                tvDesc.setTextColor(
                    getColorByAttrId(R.attr.textColorSuccess, this)
                )
                tvDesc.setText(
                    when (transactionRecord.transactionType) {
                        TransactionType.TRANSFER -> {
                            R.string.desc_transaction_state_transfer_success
                        }

                        TransactionType.COLLECTION -> {
                            R.string.desc_transaction_state_collection_success
                        }

                        TransactionType.REGISTER -> {
                            R.string.desc_transaction_state_register_success
                        }

                        else -> {
                            R.string.desc_transaction_state_transaction_success
                        }
                    }
                )
            }
        }

        tvTime.text = formatDate(transactionRecord.time, pattern = "yyyy-MM-dd HH:mm:ss")

        val amountWithUnit =
            convertAmountToDisplayUnit(transactionRecord.amount, transactionRecord.coinType)
        tvAmount.text =
            "${amountWithUnit.first} ${transactionRecord.tokenName ?: amountWithUnit.second}"

        val gasWithUnit =
            convertAmountToDisplayUnit(transactionRecord.gas, transactionRecord.coinType)
        tvGas.text = "${gasWithUnit.first} ${gasWithUnit.second}"

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
        textView.text = "— —"
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }

    private fun checkStoragePermission() {
        val perms = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (EasyPermissions.hasPermissions(this, *perms)) {
            saveIntoAlbum()
        } else {
            EasyPermissions.requestPermissions(
                PermissionRequest.Builder(this, REQUEST_CODE_SAVE_PICTURE, *perms)
                    .setRationale(R.string.save_picture_hint_need_permissions)
                    .setNegativeButtonText(R.string.action_cancel)
                    .setPositiveButtonText(R.string.action_ok)
                    .setTheme(R.style.AppAlertDialog)
                    .build()
            )
        }
    }

    private fun saveIntoAlbum() {
        launch {
            showProgress()

            val result = withContext(Dispatchers.IO) {
                val bitmap = viewConversionBitmap()
                return@withContext saveBitmap(bitmap)
            }

            delay(300)

            dismissProgress()
            if (result) {
                showToast(R.string.tips_save_into_album_success)
            } else {
                showToast(R.string.tips_save_into_album_failure)
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

    private fun saveBitmap(bitmap: Bitmap): Boolean {
        var outputStream: OutputStream? = null
        try {
            val picDir = this.getExternalFilesDir(PIC_DIR_NAME) ?: return false
            if (!picDir.exists()) {
                picDir.mkdirs()
            }

            val curTime = System.currentTimeMillis()
            val picName = "$curTime.png"
            val picPath = File(picDir, picName).absolutePath
            val contentValues = ContentValues()
            contentValues.put(MediaStore.Images.ImageColumns.DATA, picPath)
            contentValues.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, picName)
            contentValues.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/png")
            contentValues.put(MediaStore.Images.ImageColumns.DATE_ADDED, curTime / 1000)
            contentValues.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, curTime / 1000)
            contentValues.put(MediaStore.Images.ImageColumns.SIZE, bitmap.byteCount)

            val contentResolver = this.contentResolver
            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return false

            outputStream = contentResolver.openOutputStream(uri)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            return true
        } catch (e: Exception) {
            return false
        } finally {
            try {
                outputStream?.let {
                    it.flush()
                    it.close()
                }
            } catch (ignore: Exception) {
            }
            try {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            } catch (e: Exception) {
            }
        }
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
                .setTitle(getString(R.string.save_picture_title_get_permissions))
                .setRationale(getString(R.string.save_picture_hint_set_permissions))
                .setNegativeButton(R.string.action_cancel)
                .setPositiveButton(R.string.action_ok)
                .setThemeResId(R.style.AppAlertDialog)
                .build()
                .show()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        when (requestCode) {
            REQUEST_CODE_SAVE_PICTURE -> {
                saveIntoAlbum()
            }
        }
    }
}