package com.violas.wallet.ui.setting

import android.os.Bundle
import android.view.View
import com.palliums.utils.TextWatcherSimple
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import kotlinx.android.synthetic.main.activity_feedback.*

/**
 * Created by elephant on 2019-11-12 18:50.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 问题反馈页面
 */
class FeedbackActivity : BaseAppActivity() {

    companion object {
        private const val CONTENT_MAX_NUMBER = 200
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_feedback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.help_feedback_title)

        vCount.text = getString(R.string.feedback_content_count, 0, CONTENT_MAX_NUMBER)

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

                vCount.text =
                    getString(R.string.feedback_content_count, currentNumber, CONTENT_MAX_NUMBER)
            }
        })
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.vConfirm -> {
                val content = vContent.text.toString().trim()
                if (content.isEmpty()) {
                    showToast(R.string.feedback_hint_input_content)
                    return
                }

                val contact = vContact.text.toString().trim()
                if (contact.isEmpty()) {
                    showToast(R.string.feedback_hint_input_contact)
                    return
                }

                // TODO 接口调用
                finish()
            }

            R.id.vClose -> {
                finish()
            }
        }
    }
}