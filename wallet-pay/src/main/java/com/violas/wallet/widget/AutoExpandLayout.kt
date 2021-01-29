package com.violas.wallet.widget

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

class AutoExpandLayout(context: Context, attrs: AttributeSet?, defStyle: Int) :
    LinearLayout(context, attrs, defStyle) {

    companion object {
        fun setViewHeight(view: View?, height: Int) {
            val params: ViewGroup.LayoutParams? = view?.layoutParams
            params?.height = height
            view?.requestLayout()
        }
    }

    init {
        initView()
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private var layoutView: View? = null
    private var viewHeight = 0
    var isExpand = false
        private set
    private var animationDuration: Long = 0
    private fun initView() {
        layoutView = this
        isExpand = true
        animationDuration = 300
        setViewDimensions()
    }

    /**
     * @param isExpand 初始状态是否折叠
     */
    fun initExpand(isExpand: Boolean) {
        this.isExpand = isExpand
        if (!isExpand) {
            animateToggle(10)
        }
    }

    /**
     * 设置动画时间
     *
     * @param animationDuration 动画时间
     */
    fun setAnimationDuration(animationDuration: Long) {
        this.animationDuration = animationDuration
    }

    /**
     * 获取 subView 的总高度
     * View.post() 的 runnable 对象中的方法会在 View 的 measure、layout 等事件后触发
     */
    private fun setViewDimensions() {
        layoutView?.post {
            if (viewHeight <= 0) {
                viewHeight = layoutView?.measuredHeight ?: 0
            }
        }
    }

    /**
     * 重新计算View 高度
     */
    fun reSetViewDimensions() {
        layoutView?.post { viewHeight = layoutView?.measuredHeight ?: 0 }
    }

    /**
     * 切换动画实现
     */
    private fun animateToggle(animationDuration: Long) {
        val heightAnimation = if (isExpand) {
            ValueAnimator.ofFloat(0f, viewHeight.toFloat())
        } else {
            ValueAnimator.ofFloat(viewHeight.toFloat(), 0f)
        }
        heightAnimation.duration = animationDuration / 2
        heightAnimation.startDelay = animationDuration / 2
        heightAnimation.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            setViewHeight(layoutView, value.toInt())
        }
        heightAnimation.start()
    }

    /**
     * 折叠view
     */
    fun collapse() {
        isExpand = false
        animateToggle(animationDuration)
    }

    /**
     * 展开view
     */
    fun expand() {
        isExpand = true
        animateToggle(animationDuration)
    }

    fun toggleExpand() {
        if (isExpand) {
            collapse()
        } else {
            expand()
        }
    }
}