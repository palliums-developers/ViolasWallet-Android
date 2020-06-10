package com.violas.wallet.ui.transactionDetails

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.palliums.base.BaseViewHolder
import com.palliums.extensions.close
import com.palliums.extensions.show
import com.palliums.listing.ListingViewAdapter
import com.palliums.utils.CustomMainScope
import com.palliums.utils.StatusBarUtil
import com.palliums.utils.formatDate
import com.violas.wallet.R
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.ui.transactionRecord.TransactionRecordVO
import com.violas.wallet.ui.transactionRecord.TransactionState
import com.violas.wallet.ui.transactionRecord.TransactionType
import com.violas.wallet.utils.convertAmountToDisplayUnit
import kotlinx.android.synthetic.main.dialog_share_transaction_details.*
import kotlinx.android.synthetic.main.item_share_transaction_details.view.*
import kotlinx.coroutines.CoroutineScope


/**
 * Created by elephant on 2020/6/9 15:05.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 分享交易详情对话框
 */
class ShareTransactionDetailsDialog : DialogFragment(), CoroutineScope by CustomMainScope() {

    private lateinit var mTransactionRecordVO: TransactionRecordVO

    private var shareCallback: ((shareType: Int) -> Unit)? = null

    companion object {
        private const val PIC_DIR_NAME = "ViolasPay Photos"

        fun start(
            fragmentManager: FragmentManager,
            record: TransactionRecordVO,
            shareCallback: (shareType: Int) -> Unit
        ) {
            ShareTransactionDetailsDialog()
                .apply {
                    arguments = Bundle().apply { putParcelable(KEY_ONE, record) }
                    this.shareCallback = shareCallback
                }
                .show(fragmentManager)
        }
    }

    override fun onStart() {
        dialog?.window?.let {
            val attributes = it.attributes
            attributes.width = WindowManager.LayoutParams.MATCH_PARENT
            attributes.height = WindowManager.LayoutParams.MATCH_PARENT
            it.attributes = attributes

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                it.decorView.systemUiVisibility =
                    (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
                it.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                it.statusBarColor = Color.TRANSPARENT
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                it.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            }
            StatusBarUtil.setLightStatusBarMode(it, true)

            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(false)

        super.onStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_share_transaction_details, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (initData(savedInstanceState)) {
            initEvent()
            initView(mTransactionRecordVO)
        } else {
            close()
        }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        val bundle = savedInstanceState ?: arguments ?: return false
        mTransactionRecordVO = bundle.getParcelable(KEY_ONE) ?: return false
        return true
    }

    private fun initEvent() {
        tvCancel.setOnClickListener {
            close()
        }
    }

    private fun initView(record: TransactionRecordVO) {
        rvShareTypes.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        rvShareTypes.adapter = ShareViewAdapter(
            mutableListOf(
                Pair(R.drawable.sel_share_type_download, R.string.desc_save_into_album)
            )
        ) {
            close()
            shareCallback?.invoke(it)
        }

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
}

class ShareViewAdapter(
    dataList: MutableList<Pair<Int, Int>>,
    private val onItemClick: (Int) -> Unit
) : ListingViewAdapter<Pair<Int, Int>>(dataList) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<Pair<Int, Int>> {
        return ShareViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_share_transaction_details,
                parent,
                false
            ), onItemClick
        )
    }
}

class ShareViewHolder(
    view: View,
    private val onItemClick: (Int) -> Unit
) : BaseViewHolder<Pair<Int, Int>>(view) {

    init {
        itemView.setOnClickListener(this)
    }

    override fun onViewBind(itemPosition: Int, itemData: Pair<Int, Int>?) {
        itemData?.let {
            itemView.ivIcon.setImageResource(it.first)
            itemView.tvDesc.setText(it.second)
        }
    }

    override fun onViewClick(view: View, itemPosition: Int, itemData: Pair<Int, Int>?) {
        itemData?.let { onItemClick.invoke(itemPosition) }
    }
}