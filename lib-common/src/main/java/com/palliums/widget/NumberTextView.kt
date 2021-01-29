package com.palliums.widget

import android.content.Context
import android.text.font.CustomFontHelper
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class NumberTextView : AppCompatTextView {
    private val mCustomFontHelper by lazy {
        CustomFontHelper()
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        android.R.attr.textViewStyle
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        applyCustomFont(context, attrs)
    }

    private fun applyCustomFont(context: Context, attrs: AttributeSet?) {
        val textStyle = CustomFontHelper.getTextStyle(context, attrs)
        val customFont = mCustomFontHelper.selectTypeface(context, textStyle)
        typeface = customFont
    }
}