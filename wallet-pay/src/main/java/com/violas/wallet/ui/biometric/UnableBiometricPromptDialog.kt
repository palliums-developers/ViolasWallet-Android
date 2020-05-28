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
import kotlinx.android.synthetic.main.dialog_unable_biometric_prompt.*

/**
 * Created by elephant on 2020/5/28 16:05.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 无法启用生物识别提示对话框
 */
class UnableBiometricPromptDialog : DialogFragment() {

    private var promptText: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_unable_biometric_prompt, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (promptText == null) {
            close()
        } else {
            tvDesc.text = promptText
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

        dialog?.setCanceledOnTouchOutside(true)

        super.onStart()
    }

    fun setPromptText(promptText: String): UnableBiometricPromptDialog {
        this.promptText = promptText
        return this
    }
}