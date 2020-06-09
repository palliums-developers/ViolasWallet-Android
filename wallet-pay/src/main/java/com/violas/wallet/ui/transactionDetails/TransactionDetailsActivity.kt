package com.violas.wallet.ui.transactionDetails

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.yokeyword.fragmentation.SupportActivity

/**
 * Created by elephant on 2020/6/8 15:05.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易详情页面
 */
class TransactionDetailsActivity : SupportActivity(), ViewController,
    CoroutineScope by CustomMainScope() {

    companion object {
        fun start(context: Context, record: TransactionRecordVO) {
            Intent(context, TransactionDetailsActivity::class.java)
                .apply { putExtra(KEY_ONE, record) }
                .start(context)
        }
    }

    private lateinit var mTransactionRecordVO: TransactionRecordVO

    private var mLoadingDialog: LoadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_transaction_details)
        if (initData(savedInstanceState)) {
            initEvent()
            initView(mTransactionRecordVO)
        } else {
            close()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_ONE, mTransactionRecordVO)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_transaction_details, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.app_bar_share -> {
                // TODO 展示分享弹窗
                showToast(R.string.title_share)
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
            mTransactionRecordVO = record
            true
        }
    }

    private fun initEvent() {
        tvViewByBrowser.setOnClickListener {
            if (!openBrowser(this, mTransactionRecordVO.url!!)) {
                WebCommonActivity.start(this, mTransactionRecordVO.url!!)
            }
        }

        llReceiptAddress.setOnClickListener {
            if (!mTransactionRecordVO.toAddress.isBlank()) {
                ClipboardUtils.copy(this, mTransactionRecordVO.toAddress)
            }
        }

        llPaymentAddress.setOnClickListener {
            if (!mTransactionRecordVO.fromAddress.isBlank()) {
                ClipboardUtils.copy(this, mTransactionRecordVO.fromAddress)
            }
        }

        llTransactionNumber.setOnClickListener {
            if (!mTransactionRecordVO.transactionId.isBlank()) {
                ClipboardUtils.copy(this, mTransactionRecordVO.transactionId)
            }
        }
    }

    private fun initView(record: TransactionRecordVO) {
        StatusBarUtil.setLightStatusBarMode(this, true)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
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

        when (record.transactionState) {
            TransactionState.PENDING -> {
                ivState.setImageResource(R.drawable.ic_transaction_state_pending)
                tvDesc.setTextColor(com.palliums.utils.getColor(R.color.color_FAA030))
                tvDesc.setText(R.string.desc_transaction_state_transaction_pending)
            }

            TransactionState.FAILURE -> {
                ivState.setImageResource(R.drawable.ic_transaction_state_failure)
                tvDesc.setTextColor(com.palliums.utils.getColor(R.color.color_F55753))
                tvDesc.setText(
                    when (record.transactionType) {
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
                ivState.setImageResource(R.drawable.ic_transaction_state_success)
                tvDesc.setTextColor(com.palliums.utils.getColor(R.color.color_00D1AF))
                tvDesc.setText(
                    when (record.transactionType) {
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

        tvTime.text = formatDate(record.time, pattern = "yyyy-MM-dd HH:mm:ss")

        val amountWithUnit =
            convertAmountToDisplayUnit(record.amount, record.coinType)
        tvAmount.text = "${amountWithUnit.first} ${record.tokenName ?: amountWithUnit.second}"

        val gasWithUnit =
            convertAmountToDisplayUnit(record.gas, record.coinType)
        tvGas.text = "${gasWithUnit.first} ${gasWithUnit.second}"

        if (record.toAddress.isBlank()) {
            noneContent(tvReceiptAddress)
        } else {
            tvReceiptAddress.text = record.toAddress
        }

        if (record.fromAddress.isBlank()) {
            noneContent(tvPaymentAddress)
        } else {
            tvPaymentAddress.text = record.fromAddress
        }

        if (record.transactionId.isBlank()) {
            noneContent(tvTransactionNumber)
        } else {
            tvTransactionNumber.text = record.transactionId
        }
    }

    private fun noneContent(textView: TextView) {
        textView.text = "— —"
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
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
}