package com.palliums.behavior

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Created by elephant on 2020/6/4 16:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class FooterBehavior(
    context: Context,
    attrs: AttributeSet
) : CoordinatorLayout.Behavior<View>(context, attrs) {

    private val interpolator by lazy { FastOutSlowInInterpolator() }

    private var hideFlag = false

    private var sinceDirectionChange = 0

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        if (dy > 0 && sinceDirectionChange < 0 || dy < 0 && sinceDirectionChange > 0) {
            child.animate().cancel()
            sinceDirectionChange = 0
        }

        sinceDirectionChange += dy
        if (sinceDirectionChange > child.height) {
            hide(child)
        } else {
            if (sinceDirectionChange < 0) {
                show(child)
            }
        }
    }

    private fun hide(view: View) {
        if(hideFlag) return

        hideFlag = true
        val animator = view.animate()
            .translationY(view.height.toFloat())
            .setInterpolator(interpolator)
            .setDuration(200)
        animator.setListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {

            }

            override fun onAnimationCancel(animation: Animator?) {
                show(view)
            }

            override fun onAnimationStart(animation: Animator?) {

            }
        })
        animator.start()
    }

    private fun show(view: View) {
        if(!hideFlag) return

        hideFlag = false
        val animator = view.animate()
            .translationY(0f)
            .setInterpolator(interpolator)
            .setDuration(200)
        animator.setListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {

            }

            override fun onAnimationCancel(animation: Animator?) {
                hide(view)
            }

            override fun onAnimationStart(animation: Animator?) {

            }
        })
        animator.start()
    }
}