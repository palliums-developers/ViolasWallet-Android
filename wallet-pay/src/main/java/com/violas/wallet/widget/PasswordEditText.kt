package com.violas.wallet.widget

import android.content.Context
import android.text.InputType
import android.text.Spannable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.res.ResourcesCompat
import com.palliums.utils.DensityUtility
import com.violas.wallet.R


class PasswordEditText : AppCompatEditText {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var mHiddenPwd = true
    private val mShowDrawable by lazy {
        ResourcesCompat.getDrawable(resources, R.drawable.ic_password_text_show, null)?.apply {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
    }
    private val mHideDrawable by lazy {
        ResourcesCompat.getDrawable(resources, R.drawable.ic_password_text_hidden, null)?.apply {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
    }

    init {
        compoundDrawablePadding = DensityUtility.dp2px(context, 5)

        showHidePwd(mHiddenPwd, true)
    }

    private fun showHidePwd(hide: Boolean, init: Boolean) {
        val drawables = compoundDrawables

        if (hide) {
            drawables[2] = mHideDrawable
            inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
        } else {
            drawables[2] = mShowDrawable
            inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_CLASS_TEXT
        }

        setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3])

        if (!init) {
            postInvalidate()

            val editable = text
            editable?.let {
                setSelection((editable as Spannable).length)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && compoundDrawables[2] != null) {
            if (isTouchIcon(event)) {
                return true
            }
        } else if (event.action == MotionEvent.ACTION_UP && compoundDrawables[2] != null) {
            if (isTouchIcon(event)) {
                mHiddenPwd = !mHiddenPwd
                showHidePwd(mHiddenPwd, false)
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * 当手指触摸的位置在显示隐藏图标的区域，我们将此视为切换显示隐藏操作
     * width：                       控件的宽度
     * event.x：                     触摸时的坐标（该坐标是相对于控件本身而言的）
     * paddingRight：                图标右边缘到控件右边缘的距离
     * totalPaddingRight：           图标左边缘到控件右边缘的距离
     * 于是：
     * width - paddingRight：        控件左边到图标右边缘的区域
     * width - totalPaddingRight：   控件左边到图标左边缘的区域
     * 所以这两者之间的区域刚好是图标的区域
     */
    private fun isTouchIcon(event: MotionEvent): Boolean {
        return event.x > (width - totalPaddingRight - 10) &&
                event.x < (width - paddingRight + 10)
    }
}