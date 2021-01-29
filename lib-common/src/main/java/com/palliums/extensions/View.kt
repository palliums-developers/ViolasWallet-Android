package com.palliums.extensions

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.internal.CollapsingTextHelper
import com.palliums.content.ContextProvider
import com.palliums.utils.DensityUtility

fun View.expandTouchArea(dp: Int = 10) {
    val size = DensityUtility.dp2px(ContextProvider.getContext(), dp)
    val parentView: View = this.parent as View
    parentView.post {
        val rect = Rect()
        this.getHitRect(rect)
        rect.top -= size
        rect.bottom += size
        rect.left -= size
        rect.right += size
        parentView.touchDelegate = TouchDelegate(rect, this)
    }
}

fun Toolbar.setTitleToCenter(titleView: TextView): Boolean {

    return try {
        val navButtonViewField = this.javaClass.getDeclaredField("mNavButtonView")
        navButtonViewField.isAccessible = true
        val navButtonView = navButtonViewField.get(this) as? ImageButton
        navButtonViewField.isAccessible = false

        val menuViewField = this.javaClass.getDeclaredField("mMenuView")
        menuViewField.isAccessible = true
        val menuView = menuViewField.get(this) as? ActionMenuView
        menuViewField.isAccessible = false

        val navButtonViewWidth = navButtonView?.measuredWidth ?: 0
        val menuViewWidth = menuView?.measuredWidth ?: 0
        if (navButtonViewWidth > menuViewWidth) {
            val layoutParams = titleView.layoutParams as Toolbar.LayoutParams
            layoutParams.marginEnd = navButtonViewWidth
            titleView.layoutParams = layoutParams
        } else if (navButtonViewWidth < menuViewWidth) {
            val layoutParams = titleView.layoutParams as Toolbar.LayoutParams
            layoutParams.marginStart = menuViewWidth
            titleView.layoutParams = layoutParams
        }

        true
    } catch (e: Exception) {
        false
    }
}

@SuppressLint("RestrictedApi")
fun CollapsingToolbarLayout.setTitleToCenter(): Boolean {

    return try {
        val toolbarField = this.javaClass.getDeclaredField("toolbar")
        toolbarField.isAccessible = true
        val toolbar = toolbarField.get(this) as Toolbar
        toolbarField.isAccessible = false

        val navButtonViewField = toolbar.javaClass.getDeclaredField("mNavButtonView")
        navButtonViewField.isAccessible = true
        val mNavButtonView = navButtonViewField.get(toolbar) as ImageButton
        navButtonViewField.isAccessible = false

        val collapsingTextHelperField =
            this.javaClass.getDeclaredField("collapsingTextHelper")
        collapsingTextHelperField.isAccessible = true
        val collapsingTextHelper =
            collapsingTextHelperField.get(this) as CollapsingTextHelper
        collapsingTextHelperField.isAccessible = false

        val collapsedBoundsField =
            collapsingTextHelper.javaClass.getDeclaredField("collapsedBounds")
        collapsedBoundsField.isAccessible = true
        val collapsedBounds = collapsedBoundsField.get(collapsingTextHelper) as Rect
        collapsedBoundsField.isAccessible = false

        collapsingTextHelper.setCollapsedBounds(
            mNavButtonView.measuredWidth,
            collapsedBounds.top,
            this.measuredWidth - mNavButtonView.measuredWidth,
            collapsedBounds.bottom
        )
        collapsingTextHelper.recalculate()

        true
    } catch (e: Exception) {
        false
    }
}

fun BottomNavigationView.clearLongPressToast(itemIds: List<Int>) {
    val menuView = getChildAt(0) as? ViewGroup
    for (i in itemIds.indices) {
        menuView?.getChildAt(i)?.findViewById<View>(itemIds[i])?.setOnLongClickListener {
            true
        }
    }
}