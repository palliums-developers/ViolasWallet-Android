package com.palliums.widget

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.FakeBoldSpan
import android.util.AttributeSet
import android.widget.CustomFontHelper
import androidx.appcompat.widget.AppCompatTextView
import com.palliums.R

/**
 * Created by elephant on 2020/10/15 11:43.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 加强版的TextView
 * fakeBold 与 roboto 效果不会叠加，使用 fakeBold 时，自动忽略系统的 bold textStyle
 */
class EnhancedTextView : AppCompatTextView {

    private val mCustomFontHelper by lazy {
        CustomFontHelper()
    }

    private var fakeBold = false            // 伪粗体效果，比原字体加粗的效果弱一点
    private var useRoboto = false           // 使用roboto字体
    private var forceFocused = false        // 强制view有焦点

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        android.R.attr.textViewStyle
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr, 0)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        if (attrs == null) return

        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.EnhancedTextView,
            defStyleAttr,
            defStyleRes
        )

        fakeBold = typedArray.getBoolean(
            R.styleable.EnhancedTextView_etvFakeBold, fakeBold
        )
        useRoboto = typedArray.getBoolean(
            R.styleable.EnhancedTextView_etvUseRoboto, useRoboto
        )
        forceFocused = typedArray.getBoolean(
            R.styleable.EnhancedTextView_etvForceFocused, forceFocused
        )
        typedArray.recycle()

        val textStyle = CustomFontHelper.getTextStyle(context, attrs)
        if (useRoboto) {
            typeface = mCustomFontHelper.selectTypeface(context, textStyle)
        }
        if (fakeBold) {
            setTypeface(typeface, textStyle)
            text = text
        }
    }

    fun setFakeBold(fakeBold: Boolean, fakeBoldText: CharSequence? = null) {
        if (fakeBold != this.fakeBold) {
            this.fakeBold = fakeBold
            text = fakeBoldText ?: text
        }
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(
            if (text.isNullOrBlank() || text is Spanned || !fakeBold)
                text
            else
                SpannableStringBuilder(text).apply {
                    setSpan(
                        FakeBoldSpan(),
                        0,
                        text.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                },
            type
        )
    }

    override fun setTypeface(tf: Typeface?, style: Int) {
        super.setTypeface(
            when {
                fakeBold && tf == Typeface.DEFAULT_BOLD -> Typeface.DEFAULT
                else -> tf
            },
            when {
                fakeBold && style == Typeface.BOLD -> Typeface.NORMAL
                fakeBold && style == Typeface.BOLD_ITALIC -> Typeface.ITALIC
                else -> style
            }
        )
    }

    override fun isFocused(): Boolean {
        return if (forceFocused) true else super.isFocused()
    }
}