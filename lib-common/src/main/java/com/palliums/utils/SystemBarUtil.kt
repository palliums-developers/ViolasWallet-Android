package com.palliums.utils

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager

/**
 * Created by elephant on 11/19/20 4:16 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

/**
 * 设置状态栏和导航栏
 *
 * @param layoutToStatusBar         拓展布局到状态栏后面
 * @param layoutToNavigationBar     拓展布局到导航栏后面
 * @param lightModeStatusBar        浅色模式状态栏，支持6.0系统及以上，6.0系统以下为深色模式状态栏
 * @param lightModeNavigationBar    浅色模式导航栏，支持8.0系统及以上，8.0系统以下为深色模式导航栏
 * @param statusBarColorAboveM      6.0系统及以上的状态栏的颜色
 * @param navigationBarColorAboveO  8.0系统及以上的导航栏的颜色
 * @param statusBarColorBelowM      6.0系统以下的状态栏的颜色
 * @param navigationBarColorBelowO  8.0系统以下的导航栏的颜色
 * @param hideStatusBar             隐藏状态栏
 * @param hideNavigationBar         隐藏导航栏
 * @param immersiveStickyMode       沉浸模式
 * @param lowProfileMode            低调模式
 *
 */
fun Window.setSystemBar(
    layoutToStatusBar: Boolean = true,
    layoutToNavigationBar: Boolean = false,
    lightModeStatusBar: Boolean = true,
    lightModeNavigationBar: Boolean = true,
    statusBarColorAboveM: Int = Color.TRANSPARENT,
    navigationBarColorAboveO: Int = Color.TRANSPARENT,
    statusBarColorBelowM: Int = Color.parseColor("#40000000"),
    navigationBarColorBelowO: Int = Color.BLACK,
    hideStatusBar: Boolean? = null,
    hideNavigationBar: Boolean? = null,
    immersiveStickyMode: Boolean? = null,
    lowProfileMode: Boolean? = null
) {
    // 需要设置这个才能设置状态栏和导航栏颜色
    addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    // 移除隐藏状态栏的Flag
    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    // 移除隐藏导航栏的Flag
    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

    // View.SYSTEM_UI_FLAG_LAYOUT_STABLE            稳定的布局，不会随系统栏的隐藏、显示而变化
    // View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN        拓展布局到状态栏后面
    // View.SYSTEM_UI_FLAG_FULLSCREEN               隐藏状态栏，需要从状态栏位置下拉才会出现
    // View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION   拓展布局到导航栏后面
    // View.SYSTEM_UI_FLAG_HIDE_NAVIGATION          隐藏导航栏，用户点击屏幕会显示导航栏
    // View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY         沉浸模式，用户可以交互的界面。同时，用户上下拉系统栏时，会自动隐藏系统栏
    // View.SYSTEM_UI_FLAG_IMMERSIVE                沉浸模式，用户可以交互的界面
    // View.SYSTEM_UI_FLAG_LOW_PROFILE              低调模式，弱化状态栏和导航栏的图标
    var systemUiFlag = decorView.systemUiVisibility or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE and
            View.SYSTEM_UI_FLAG_IMMERSIVE.inv()

    // 拓展布局到状态栏后面
    systemUiFlag = if (layoutToStatusBar)
        systemUiFlag or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    else
        systemUiFlag and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN.inv()


    // 浅色深色状态栏设置，因为从6.0系统才开始支持浅色状态栏模式，所以6.0系统以下统一使用深色状态栏
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        statusBarColor = statusBarColorAboveM

        systemUiFlag = if (lightModeStatusBar)
            systemUiFlag or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        else
            systemUiFlag and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    } else {
        statusBarColor = statusBarColorBelowM
    }

    // 拓展布局到导航栏后面
    systemUiFlag = if (layoutToNavigationBar)
        systemUiFlag or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    else
        systemUiFlag and View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION.inv()

    // 浅色深色导航栏设置，因为从8.0系统才开始支持浅色导航栏模式，所以8.0系统以下统一使用深色导航栏
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        navigationBarColor = navigationBarColorAboveO

        systemUiFlag = if (lightModeNavigationBar)
            systemUiFlag or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        else
            systemUiFlag and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
    } else {
        navigationBarColor = navigationBarColorBelowO
    }

    // 显示隐藏状态栏
    hideStatusBar?.let {
        systemUiFlag = if (it)
            systemUiFlag or View.SYSTEM_UI_FLAG_FULLSCREEN
        else
            systemUiFlag and View.SYSTEM_UI_FLAG_FULLSCREEN.inv()
    }

    // 显示隐藏导航栏
    hideNavigationBar?.let {
        systemUiFlag = if (it)
            systemUiFlag or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        else
            systemUiFlag and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv()
    }

    // 沉浸模式
    immersiveStickyMode?.let {
        systemUiFlag = if (it)
            systemUiFlag or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        else
            systemUiFlag and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY.inv()
    }

    // 低调模式
    lowProfileMode?.let {
        systemUiFlag = if (it)
            systemUiFlag or View.SYSTEM_UI_FLAG_LOW_PROFILE
        else
            systemUiFlag and View.SYSTEM_UI_FLAG_LOW_PROFILE.inv()
    }

    decorView.systemUiVisibility = systemUiFlag
}

/**
 * 浅色模式状态栏
 */
fun Window.lightModeStatusBar(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return false
    }

    decorView.systemUiVisibility =
        decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    return true
}

/**
 * 深色模式状态栏
 */
fun Window.darkModeStatusBar(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return true
    }

    decorView.systemUiVisibility =
        decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    return true
}

/**
 * 浅色模式导航栏
 */
fun Window.lightModeNavigationBar(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return false
    }

    decorView.systemUiVisibility =
        decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    return true
}

/**
 * 深色模式导航栏
 */
fun Window.darkModeNavigationBar(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return true
    }

    decorView.systemUiVisibility =
        decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
    return true
}