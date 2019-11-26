package com.violas.wallet.ui.selectCurrency.view


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.ceil

class QuickIndexBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint: Paint = Paint()
    private var mCellHeight: Float = 0.toFloat()
    private var mWidth: Int = 0

    private var mHeight: Int = 0
    private val mTextHeight: Float
    private var currentIndex = -1

    var onLetterChangeListener: OnLetterChangeListener? = null
        set(value) {
            field = value
        }

    //暴露接口
    interface OnLetterChangeListener {
        fun onLetterChange(letter: String)

        fun onReset()
    }

    init {

        // 画笔默认是 黑色  设置为白色

        //设置字体大小
        paint.textSize = dip2px(context, 14f).toFloat()

        //抗锯齿
        paint.isAntiAlias = true

        // 获取字体的高度
        val fontMetrics = paint.fontMetrics

        //  下边界  - 上边界
        //ceil 天花板    0.1  1
        mTextHeight = ceil((fontMetrics.descent - fontMetrics.ascent).toDouble()).toFloat()
    }

    // 测量完成  改变的时候调用
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        //获取测量后的宽度和高度
        mWidth = measuredWidth
        mHeight = measuredHeight

        //每个字母的高度
        mCellHeight = mHeight * 1.0f / LETTERS.size
    }

    // 怎么画
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //遍历并绘制26英文字母
        for (i in LETTERS.indices) {
            val text = LETTERS[i]

            //测量字体宽度
            val mTextWidth = paint.measureText(text)

            //获取字母的xy坐标，坐标默认为字母左下角
            val x = mWidth / 2 - mTextWidth / 2
            val y = mCellHeight / 2 + mTextHeight / 2 + mCellHeight * i

            //判断当前索引并绘制相应的颜色
            if (currentIndex == i) {
                //当索引为当前的字母时绘制的颜色
                paint.color = Color.parseColor("#ff933e")
            } else {
                paint.color = Color.parseColor("#454545")
            }
            // 字.画字()
            canvas.drawText(text, x, y, paint)
        }
    }

    //触摸事件
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 计算当前点击的 字母
                val downY = event.y
                // 1.1  ---  1  1.4 --- 1  1.5 --- 1
                currentIndex = (downY / mCellHeight).toInt()
                if (currentIndex < 0 || currentIndex > LETTERS.size - 1) {
                } else {
                    //                    Utils.showToast(getContext(), LETTERS[currentIndex]);
                    if (onLetterChangeListener != null) {
                        onLetterChangeListener!!.onLetterChange(LETTERS[currentIndex])
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // 计算当前点击的 字母
                val moveY = event.y
                currentIndex = (moveY / mCellHeight).toInt() // 1.1  ---  1  1.4 --- 1  1.5 --- 1

                if (currentIndex < 0 || currentIndex > LETTERS.size - 1) {
                } else {
                    if (onLetterChangeListener != null) {
                        onLetterChangeListener!!.onLetterChange(LETTERS[currentIndex])
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                currentIndex = -1
                if (onLetterChangeListener != null) {
                    onLetterChangeListener!!.onReset()
                }
            }
        }//重新绘制
        //                invalidate();
        //重新绘制
        //                invalidate();
        //重新绘制
        invalidate()

        //   返回true  为了收到 move  & up 事件
        return true
    }

    companion object {

        //26英文字母
        private val LETTERS = arrayOf(
            "A",
            "B",
            "C",
            "D",
            "E",
            "F",
            "G",
            "H",
            "I",
            "J",
            "K",
            "L",
            "M",
            "N",
            "O",
            "P",
            "Q",
            "R",
            "S",
            "T",
            "U",
            "V",
            "W",
            "X",
            "Y",
            "Z",
            "#",
            " "
        )

        /**
         * 根据手机的分辨率从 dip 的单位 转成为 px(像素)
         */
        fun dip2px(context: Context, dpValue: Float): Int {
            val scale = context.applicationContext.resources.displayMetrics.density
            return (dpValue * scale + 0.5f).toInt()
        }
    }
}

