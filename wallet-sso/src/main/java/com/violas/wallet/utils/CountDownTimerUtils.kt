package com.violas.wallet.utils

import android.graphics.Color
import android.os.CountDownTimer
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import com.palliums.utils.getString
import com.violas.wallet.R

class CountDownTimerUtils(
    private val mTextView: TextView //显示倒计时的文字
    , millisInFuture: Long,
    countDownInterval: Long
) : CountDownTimer(millisInFuture, countDownInterval) {

    override fun onTick(millisUntilFinished: Long) {
        mTextView.isClickable = false //设置不可点击
        mTextView.isEnabled = false

        val resend = getString(R.string.resend)
        val time = "(" + (millisUntilFinished / 1000).toString() + "s)"
        val spannableString = SpannableString(resend + time).also {
            it.setSpan(
                ForegroundColorSpan(Color.RED),
                resend.length,
                it.length,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        mTextView.text = spannableString //设置倒计时时间
    }

    override fun onFinish() {
        mTextView.text = getString(R.string.get_verification_code)
        mTextView.isClickable = true//重新获得点击
        mTextView.isEnabled = true
    }
}