package com.palliums.extensions

import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.Toolbar.LayoutParams

/**
 * Created by elephant on 2020/6/4 18:06.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

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
            val layoutParams = titleView.layoutParams as LayoutParams
            layoutParams.marginEnd = navButtonViewWidth
            titleView.layoutParams = layoutParams
        } else if (navButtonViewWidth < menuViewWidth) {
            val layoutParams = titleView.layoutParams as LayoutParams
            layoutParams.marginStart = menuViewWidth
            titleView.layoutParams = layoutParams
        }

        true
    } catch (e: Exception) {
        false
    }
}