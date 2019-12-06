package com.palliums.utils

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.tabs.TabLayout
import com.palliums.R
import java.lang.reflect.Field


/**
 * Created by elephant on 2019-11-14 17:29.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

private const val VIEW_CLICK_INTERVAL_TIME = 800

/**
 * 判断当前的点击事件是否是快速多次点击(连续多点击），该方法用来防止多次连击。
 *
 * @param view 被点击view，如果前后是同一个view，则进行双击校验
 * @return 认为是重复点击时返回 true，当连续点击时，如果距上一次有效点击时间超过了 INTERVAL_TIME 则返回 false
 */
fun isFastMultiClick(view: View?): Boolean {
    return isFastMultiClick(view, VIEW_CLICK_INTERVAL_TIME.toLong())
}

/**
 * 判断当前的点击事件是否是快速多次点击(连续多点击），该方法用来防止多次连击。
 *
 * @param view     被点击view，如果前后是同一个view，则进行双击校验
 * @param duration 两次点击的最小间隔（单位：毫秒），必须大于 0 否则将返回 false。
 * @return 认为是重复点击时返回 true，当连续点击时，如果距上一次有效点击时间超过了 duration 则返回 false
 */
fun isFastMultiClick(view: View?, duration: Long): Boolean {
    if (view == null || duration < 1) {
        return false
    }

    val pervTime = view.getTag(R.id.view_click_time)?.let { it as Long }

    if (pervTime != null && System.currentTimeMillis() - pervTime < duration) {
        return true
    }

    view.setTag(R.id.view_click_time, System.currentTimeMillis())

    return false
}

fun drawTextInRect(
    canvas: Canvas,
    text: String,
    rect: Rect,
    paint: Paint
) {
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    val height = rect.bottom - rect.top.toFloat()
    val width = rect.right - rect.left.toFloat()
    canvas.drawText(
        text,
        rect.left + width / 2 - bounds.width() / 2 - bounds.left,
        rect.top + height / 2 + bounds.height() / 2,
        paint
    )
}

/**
 * 调节TabLayout指示线宽度
 * @param leftDip
 * @param rightDip
 */
fun TabLayout.setIndicatorLineWidth(leftDip: Float, rightDip: Float) {

    val indicatorContainer: LinearLayout = try {
        val slidingTabIndicator: Field = this.javaClass.getDeclaredField("slidingTabIndicator")
        slidingTabIndicator.isAccessible = true

        slidingTabIndicator.get(this) as LinearLayout
    } catch (e: Exception) {
        e.printStackTrace()

        return
    }

    val left = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        leftDip,
        Resources.getSystem().displayMetrics
    ).toInt()

    val right = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        rightDip,
        Resources.getSystem().displayMetrics
    ).toInt()

    for (i in 0 until indicatorContainer.childCount) {

        val child: View = indicatorContainer.getChildAt(i)
        child.setPadding(0, 0, 0, 0)

        val params: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)

        params.leftMargin = left
        params.rightMargin = right

        child.layoutParams = params
        child.invalidate()
    }
}