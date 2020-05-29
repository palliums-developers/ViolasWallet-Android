package com.violas.wallet.ui.biometric

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.palliums.extensions.close
import com.violas.wallet.R
import kotlinx.android.synthetic.main.dialog_open_biometrics_prompt.*

/**
 * Created by elephant on 2020/5/29 16:21.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 开启指纹识别提示对话框
 */
class OpenBiometricsPromptDialog : DialogFragment() {

    private var confirmCallback: (() -> Unit)? = null
    private var cancelCallback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_open_biometrics_prompt, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnPositive.setOnClickListener {
            close()
            confirmCallback?.invoke()
        }

        tvNegative.setOnClickListener {
            close()
            cancelCallback?.invoke()
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

    fun setCallback(
        cancelCallback: (() -> Unit)? = null,
        confirmCallback: () -> Unit
    ): OpenBiometricsPromptDialog {
        this.cancelCallback = cancelCallback
        this.confirmCallback = confirmCallback
        return this
    }
}