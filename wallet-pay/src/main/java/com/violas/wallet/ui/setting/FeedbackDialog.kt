package com.violas.wallet.ui.setting

import android.os.Bundle
import android.view.Gravity
import android.view.View
import com.palliums.base.BaseDialogFragment
import com.palliums.utils.TextWatcherSimple
import com.violas.wallet.R
import kotlinx.android.synthetic.main.dialog_feedback.*

/**
 * Created by elephant on 2019-11-01 14:17.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 问题反馈弹窗
 */
class FeedbackDialog : BaseDialogFragment() {

    companion object {
        private const val CONTENT_MAX_NUMBER = 200
    }

    override fun getLayoutResId(): Int {
        return R.layout.dialog_feedback
    }

    override fun getWindowAnimationsStyleId(): Int {
        return R.style.AnimationDefaultCenterDialog
    }

    override fun getWindowLayoutParamsGravity(): Int {
        return Gravity.CENTER
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vCount.text = getString(
            R.string.feedback_desc_content_count,
            0,
            CONTENT_MAX_NUMBER
        )

        vClose.setOnClickListener(this)
        vConfirm.setOnClickListener(this)

        vContent.addTextChangedListener(object : TextWatcherSimple() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentNumber = s?.length ?: 0
                if (currentNumber > CONTENT_MAX_NUMBER) {
                    vContent.setText(s.toString().substring(0, CONTENT_MAX_NUMBER))
                    vContent.setSelection(CONTENT_MAX_NUMBER)
                    return
                }

                vCount.text = getString(
                    R.string.feedback_desc_content_count,
                    currentNumber,
                    CONTENT_MAX_NUMBER
                )
            }
        })
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.vConfirm -> {
                val content = vContent.text.toString().trim()
                if (content.isEmpty()) {
                    showToast(R.string.feedback_tips_content_empty)
                    return
                }

                val contact = vContact.text.toString().trim()
                if (contact.isEmpty()) {
                    showToast(R.string.feedback_tips_contact_empty)
                    return
                }

                // TODO 接口调用
                close()
            }

            R.id.vClose -> {
                close()
            }
        }
    }
}