package com.violas.wallet.ui.biometric

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.palliums.extensions.close
import com.violas.wallet.R
import kotlinx.android.synthetic.main.dialog_close_biometric_payment.*

/**
 * Created by elephant on 2020/5/28 15:04.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 关闭生物支付对话框
 */
class CloseBiometricPaymentDialog : DialogFragment() {

    private var confirmCallback: (() -> Unit)? = null
    private var cancelCallback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_close_biometric_payment, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvNegativeBtn.setOnClickListener {
            close()
            cancelCallback?.invoke()
        }

        tvPositiveBtn.setOnClickListener {
            close()
            confirmCallback?.invoke()
        }
    }

    override fun onStart() {
        dialog?.window?.let {
            val point = Point()
            it.windowManager.defaultDisplay.getSize(point)

            val attributes = it.attributes
            attributes.y = (point.y * 0.23).toInt()
            it.attributes = attributes

            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        }

        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(false)

        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            it.isFocusableInTouchMode = true
            it.requestFocus()
            it.setOnKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    close()
                    cancelCallback?.invoke()
                    return@setOnKeyListener true
                }

                return@setOnKeyListener false
            }
        }
    }

    fun setCallback(
        confirmCallback: () -> Unit,
        cancelCallback: () -> Unit
    ): CloseBiometricPaymentDialog {
        this.confirmCallback = confirmCallback
        this.cancelCallback = cancelCallback
        return this
    }
}