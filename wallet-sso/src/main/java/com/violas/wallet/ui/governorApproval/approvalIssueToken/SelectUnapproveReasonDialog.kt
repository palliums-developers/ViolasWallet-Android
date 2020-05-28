package com.violas.wallet.ui.governorApproval.approvalIssueToken

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.palliums.extensions.close
import com.palliums.utils.DensityUtility
import com.palliums.utils.hideSoftInput
import com.violas.wallet.R
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.repository.http.governor.UnapproveReasonDTO
import kotlinx.android.synthetic.main.dialog_select_unapprove_reason.*

/**
 * Created by elephant on 2020/4/27 22:04.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 选择审核未通过SSO发币申请原因的弹窗
 */
class SelectUnapproveReasonDialog : DialogFragment() {

    companion object {
        fun newInstance(unapproveReasons: List<UnapproveReasonDTO>): SelectUnapproveReasonDialog {
            val dialog =
                SelectUnapproveReasonDialog()
            dialog.arguments = Bundle().apply {
                putParcelableArrayList(KEY_ONE, ArrayList(unapproveReasons))
            }
            return dialog
        }
    }

    private lateinit var mUnappoveReasons: List<UnapproveReasonDTO>
    private var mOnConfirmCallback: ((Int, String) -> Unit)? = null
    private var mSelectedReasonType = Int.MIN_VALUE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_select_unapprove_reason, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mUnappoveReasons = arguments!!.getParcelableArrayList(KEY_ONE)!!
        initView()
        initEvent()
    }

    override fun onStart() {
        dialog?.window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setGravity(Gravity.TOP or Gravity.CENTER)

            val point = Point()
            it.windowManager.defaultDisplay.getSize(point)

            val attributes = it.attributes
            //attributes.width = WindowManager.LayoutParams.MATCH_PARENT
            //attributes.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes.y = (point.y * 0.3).toInt()
            it.attributes = attributes

            it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }

        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(false)
        super.onStart()
    }

    fun setOnConfirmCallback(
        callback: (reasonType: Int, remark: String) -> Unit
    ): SelectUnapproveReasonDialog {
        this.mOnConfirmCallback = callback
        return this
    }

    private fun initView() {
        val descList = mUnappoveReasons.map { it.desc }.toMutableList()
        descList.add(getString(R.string.hint_select_unapprove_reason))
        val adapter =
            UnapproveReasonArrayAdapter(
                context!!,
                descList
            )
        spinnerReasons.dropDownVerticalOffset = DensityUtility.dp2px(context, 36)
        spinnerReasons.adapter = adapter
        spinnerReasons.setSelection(descList.size - 1)
        spinnerReasons.post{
            spinnerReasons.dropDownWidth = spinnerReasons.measuredWidth
        }
    }

    private fun initEvent() {
        rlClose.setOnClickListener {
            close()
        }

        spinnerReasons.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // ignore
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position == mUnappoveReasons.size) {
                    (view as? TextView)?.let {
                        it.hint = it.text
                        it.text = ""
                    }
                    return
                }

                val unapproveReason = mUnappoveReasons[position]
                if (unapproveReason.type == mSelectedReasonType) return

                if (mSelectedReasonType == -1) {
                    etRemark.clearComposingText()
                    etRemark.visibility = View.GONE
                    hideSoftInput(etRemark)
                } else if (unapproveReason.type == -1) {
                    etRemark.visibility = View.VISIBLE
                }

                tvTips.visibility = View.GONE
                mSelectedReasonType = unapproveReason.type
            }
        }

        tvConfirm.setOnClickListener {
            if (mSelectedReasonType == Int.MIN_VALUE) {
                tvTips.setText(R.string.hint_select_unapprove_reason)
                tvTips.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (mSelectedReasonType == -1) {
                val remark = etRemark.text.toString().trim()
                if (remark.isEmpty()) {
                    tvTips.setText(R.string.hint_remark_unapprove_reason)
                    tvTips.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                mOnConfirmCallback?.invoke(mSelectedReasonType, remark)
            } else {
                mOnConfirmCallback?.invoke(mSelectedReasonType, "")
            }

            close()
        }

        etRemark.doAfterTextChanged {
            if (tvTips.visibility == View.VISIBLE
                && etRemark.text.toString().trim().isNotEmpty()
            ) {
                tvTips.visibility == View.GONE
            }
        }
    }
}

class UnapproveReasonArrayAdapter<T>(
    context: Context, objects: List<T>
) : ArrayAdapter<T>(context, R.layout.item_select_unapprove_rease_spinner, objects) {

    init {
        setDropDownViewResource(R.layout.item_select_unapprove_rease_spinner_dropdown)
    }

    override fun getCount(): Int {
        val count = super.getCount()
        return if (count > 0) count - 1 else count
    }
}