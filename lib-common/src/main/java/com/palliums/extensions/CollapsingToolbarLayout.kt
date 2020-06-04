package com.palliums.extensions

import android.annotation.SuppressLint
import android.graphics.Rect
import android.widget.ImageButton
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.internal.CollapsingTextHelper

/**
 * Created by elephant on 2020/6/4 17:36.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

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