package android.widget

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.FontCache.getTypeface

object FontCache {
    private val fontCache: HashMap<String, Typeface?> = HashMap()
    fun getTypeface(fontname: String, context: Context): Typeface? {
        var typeface = fontCache[fontname]
        if (typeface == null) {
            typeface = try {
                Typeface.createFromAsset(context.assets, fontname)
            } catch (e: Exception) {
                return null
            }
            fontCache[fontname] = typeface
        }
        return typeface
    }
}

class CustomFontHelper {
    companion object {
        private const val ANDROID_SCHEMA = "http://schemas.android.com/apk/res/android"

        fun getTextStyle(context: Context, attrs: AttributeSet?): Int {
            return attrs?.getAttributeIntValue(
                ANDROID_SCHEMA,
                "textStyle",
                Typeface.NORMAL
            ) ?: return Typeface.NORMAL
        }
    }

    fun selectTypeface(context: Context, textStyle: Int?): Typeface? {
        /*
        * information about the TextView textStyle:
        * http://developer.android.com/reference/android/R.styleable.html#TextView_textStyle
        */
        return when (textStyle) {
            Typeface.BOLD -> getTypeface("font/roboto_medium.ttf", context)
            Typeface.BOLD_ITALIC -> getTypeface("font/roboto_medium_italic.ttf", context)
            Typeface.ITALIC -> getTypeface("font/roboto_italic.ttf", context)
            Typeface.NORMAL -> getTypeface("font/roboto_regular.ttf", context)
            else -> getTypeface("font/roboto_regular.ttf", context)
        }
    }
}

class NumberEditText : androidx.appcompat.widget.AppCompatEditText {
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