package android.widget

import android.content.Context
import android.util.AttributeSet

class NumberTextView : androidx.appcompat.widget.AppCompatTextView {
    private val mCustomFontHelper by lazy {
        CustomFontHelper()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        applyCustomFont(context, attrs)
    }

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