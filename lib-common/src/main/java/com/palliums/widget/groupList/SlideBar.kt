package com.palliums.widget.groupList

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.palliums.R
import com.palliums.utils.DensityUtility
import com.palliums.utils.drawTextInRect
import com.palliums.utils.isMainThread
import java.util.*

/**
 * Created by elephant on 2019-11-27 11:41.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 快速索引
 */
class SlideBar : View {

    private var mKeys = ArrayList<String>()

    private var mPaint = Paint()
    private val mKeyRect = Rect() //单个字符宽高
    private var mCurrent: Rect? = null

    private var mKeyView = TextView(context)//选中显示的框框
    private var mWindowParams = WindowManager.LayoutParams()
    private var mWindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var mValueAnimator: ValueAnimator? = null
    private var mIsAddWindow = false
    private var mThisWindowY = 0 //当前View所在的Window的Y点


    private var mDefaultTypeface = Typeface.create(
        Typeface.DEFAULT,
        Typeface.NORMAL
    )
    private var mBoldTypeface = Typeface.create(
        Typeface.DEFAULT,
        Typeface.BOLD
    )

    var indexColorNormal = Color.BLACK
    var indexColorSelected = Color.BLACK

    private var mOnKeyCheckedListener: OnKeyCheckedListener? = null

    constructor(context: Context) : super(context) {
        init(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        init(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs, 0, 0)
    }

    private fun init(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        mPaint.isDither = true
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
        mPaint.strokeWidth = 1f
        mPaint.textSize = DensityUtility.sp2px(context, 12f)

        val fontMetricsInt = mPaint.fontMetricsInt
        mKeyRect.left = 0
        mKeyRect.right = (mPaint.measureText("A") * 2.5f).toInt()
        mKeyRect.top = 0
        mKeyRect.bottom =
            fontMetricsInt.bottom - fontMetricsInt.top + DensityUtility.dp2px(context, 2)

        mKeyView.setTextColor(Color.WHITE)
        mKeyView.gravity = Gravity.CENTER
        mKeyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        mKeyView.setBackgroundResource(R.drawable.bg_slide_bar_key)
        mKeyView.gravity = Gravity.CENTER
        mKeyView.setPadding(0, 0, DensityUtility.dp2px(context, 5), 0)

        mWindowParams.token = applicationWindowToken
        //设置类型为应用类型,不需要权限
        mWindowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
        //设置图片格式，效果为背景透明
        mWindowParams.format = PixelFormat.RGBA_8888
        //设置浮动窗口不可触摸
        mWindowParams.flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        //调整悬浮窗显示的停靠位置
        mWindowParams.gravity = Gravity.TOP or Gravity.END
        //设置悬浮窗口长宽数据
        mWindowParams.width = DensityUtility.dp2px(context, 50)
        mWindowParams.height = DensityUtility.dp2px(context, 41)
        //以屏幕左上角为原点，设置x、y初始值，相对于gravity
        mWindowParams.x = 0
        mWindowParams.y = 0

        val widthSpec = MeasureSpec.makeMeasureSpec(
            mWindowParams.width,
            MeasureSpec.EXACTLY
        )
        val heightSpec = MeasureSpec.makeMeasureSpec(
            mWindowParams.height,
            MeasureSpec.EXACTLY
        )
        mKeyView.measure(widthSpec, heightSpec)
    }

    /**
     * 设置数据
     * @param keys
     */
    fun setData(keys: List<String>) {
        if (isMainThread()) {
            mKeys.clear()
            mKeys.addAll(keys)
            requestLayout()
        } else {
            post { setData(keys) }
        }
    }

    fun clear() {
        if (isMainThread()) {
            mKeys.clear()
            requestLayout()
        } else {
            post { clear() }
        }
    }

    /**
     * 设置监听
     * @param listener
     */
    fun setOnKeyCheckedListener(listener: OnKeyCheckedListener) {
        mOnKeyCheckedListener = listener
    }

    /**
     * 设置当前Key
     * @param key
     */
    fun setCurrentKey(key: String?) {
        val index = mKeys.indexOf(key)
        showKey(index, true)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        //如果宽度是精确值,则使用精确值,否则使用一个字符的宽度
        val measureWidth = if (widthMode == MeasureSpec.EXACTLY) {
            width
        } else {
            mKeyRect.width()
        }

        //如果高度是精确值,则使用精确值,否则一个字符的宽度 * n
        val measureHeight = if (heightMode == MeasureSpec.EXACTLY) {
            height
        } else {
            mKeys.size * mKeyRect.height()
        }

        setMeasuredDimension(
            measureWidth + paddingLeft + paddingRight,
            measureHeight + paddingBottom + paddingTop
        )
    }

    override fun onDraw(canvas: Canvas) {
        val rect = Rect()
        var startY = 0
        for (i in mKeys.indices) {
            rect.left = paddingLeft
            rect.top = startY
            rect.right = width
            rect.bottom = startY + mKeyRect.height()
            //当前点击的字符
            if (rect == mCurrent) {
                mPaint.typeface = mBoldTypeface
                mPaint.color = indexColorSelected
            } else {
                mPaint.typeface = mDefaultTypeface
                mPaint.color = indexColorNormal
            }
            drawTextInRect(canvas, mKeys[i], rect, mPaint)
            startY += mKeyRect.height()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val location = IntArray(2)
        getLocationInWindow(location)
        mThisWindowY = location[1]
        mWindowParams.x = width + DensityUtility.dp2px(context, 15)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                val y = event.y
                var index = (y / mKeyRect.height()).toInt()
                if (index >= mKeys.size) {
                    index = mKeys.size - 1
                }
                showKey(index, false)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                hideKeyView()
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mIsAddWindow) {
            mWindowManager.removeView(mKeyView)
            mIsAddWindow = false
        }
    }

    private fun showKey(index: Int, fromSetting: Boolean) {
        if (index >= 0 && index < mKeys.size) {
            val rect = Rect(
                paddingLeft,
                index * mKeyRect.height(),
                measuredWidth,
                (index + 1) * mKeyRect.height()
            )
            if (rect == mCurrent) {
                return
            }

            mValueAnimator?.let {
                if (it.isRunning) {
                    it.cancel()
                    mValueAnimator = null
                }
            }

            mCurrent = rect
            val key = mKeys[index]
            if (!fromSetting) {
                if (!mIsAddWindow) {
                    mIsAddWindow = true
                    mWindowManager.addView(mKeyView, mWindowParams)
                }
                mKeyView.alpha = 1f
                mKeyView.text = key
                mKeyView.visibility = VISIBLE
                mWindowParams.y =
                    mThisWindowY - mKeyView.measuredHeight / 2 + rect.top + rect.height() / 2
                mWindowManager.updateViewLayout(mKeyView, mWindowParams)
            }

            mOnKeyCheckedListener?.let {
                post { it.onKeyChecked(key, fromSetting) }
            }

        } else {

            mCurrent = null
        }

        invalidate()
    }


    private fun hideKeyView() {
        if (!mIsAddWindow) {
            return
        }

        mValueAnimator?.let {
            if (it.isRunning) {
                it.cancel()
                mValueAnimator = null
            }
        }

        val valueAnimator = ObjectAnimator
            .ofFloat(mKeyView, "alpha", mKeyView.alpha, 0f)
            .also {
                it.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        mKeyView.visibility = GONE
                    }
                })
                it.duration = 1500
            }
        valueAnimator.start()

        mValueAnimator = valueAnimator
    }


    interface OnKeyCheckedListener {
        /**
         * key选中回调
         * @param key
         * @param fromSetting 如果是调用 [.setCurrentKey] 改变的key,则为true,否则为false
         */
        fun onKeyChecked(key: String, fromSetting: Boolean)
    }
}