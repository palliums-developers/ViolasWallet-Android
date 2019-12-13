package com.violas.wallet.widget.dialog

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.violas.wallet.R
import kotlinx.android.synthetic.main.dialog_password_input.view.*
import me.yokeyword.fragmentation.SupportHelper

class DeleteAddressDialog : DialogFragment() {
    private lateinit var mRootView: View

    private var confirmCallback: ((DialogFragment) -> Unit)? = null
    private var cancelCallback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.TOP or Gravity.CENTER)

            val params = attributes;
            val windowManager = windowManager
            val display = windowManager.defaultDisplay
            val point = Point()
            display.getSize(point)
            params?.y = (point.y * 0.23).toInt()
            dialog?.window?.attributes = params
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }
        mRootView = inflater.inflate(R.layout.dialog_delete_address_book, container)
        isCancelable = false
        mRootView.btnConfirm.setOnClickListener {
            confirmCallback?.invoke(this)
        }
        mRootView.btnCancel.setOnClickListener {
            dismiss()
            cancelCallback?.invoke()
        }
        return mRootView
    }

    fun show(manager: FragmentManager) {
        show(manager, "password")
    }

    fun setConfirmListener(callback: (DialogFragment) -> Unit): DeleteAddressDialog {
        confirmCallback = callback
        return this
    }

    fun setCancelListener(callback: () -> Unit): DeleteAddressDialog {
        cancelCallback = callback
        return this
    }
}