package com.violas.wallet.base

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.IntDef
import com.palliums.base.BaseActivity
import com.violas.wallet.R
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility

abstract class BaseAppActivity : BaseActivity() {

    companion object {
        /**
         * 主浅色背景
         */
        const val PAGE_STYLE_PLIGHT_BACKGROUND = 1
        /**
         * 深色背景
         */
        const val PAGE_STYLE_DARK_BACKGROUND = 2
        /**
         * 深色背景且没有title
         */
        const val PAGE_STYLE_DARK_BACKGROUND_NO_TITLE = 3
        /**
         * 主浅色title 副浅色content
         */
        const val PAGE_STYLE_PLIGHT_TITLE_SLIGHT_CONTENT = 4
        /**
         * 深色title 主浅色content
         */
        const val PAGE_STYLE_DARK_TITLE_PLIGHT_CONTENT = 5
        /**
         * 自定义（会隐藏title）
         */
        const val PAGE_STYLE_CUSTOM = 6
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setPageStyle(getPageStyle())
    }

    @PageStyle
    open fun getPageStyle(): Int {
        return PAGE_STYLE_PLIGHT_BACKGROUND
    }

    @IntDef(
        PAGE_STYLE_PLIGHT_BACKGROUND,
        PAGE_STYLE_DARK_BACKGROUND,
        PAGE_STYLE_DARK_BACKGROUND_NO_TITLE,
        PAGE_STYLE_PLIGHT_TITLE_SLIGHT_CONTENT,
        PAGE_STYLE_DARK_TITLE_PLIGHT_CONTENT,
        PAGE_STYLE_CUSTOM
    )
    annotation class PageStyle

    private fun setPageStyle(@PageStyle style: Int) {
        when (style) {

            PAGE_STYLE_PLIGHT_BACKGROUND -> {
                StatusBarMode(this, true)

                setRootBackgroundColor(R.color.def_page_bg_light_primary)

                setTitleLeftImageResource(R.drawable.ic_back_dark)
                setTitleRightTextColor(R.color.def_text_font_black)
                titleColor = R.color.def_text_font_black
            }

            PAGE_STYLE_DARK_BACKGROUND -> {
                setRootBackgroundColor(R.color.def_page_bg_dark)

                setTitleLeftImageResource(R.drawable.ic_back_light)
                setTitleRightTextColor(R.color.white)
                titleColor = R.color.white
            }

            PAGE_STYLE_DARK_BACKGROUND_NO_TITLE -> {
                setRootBackgroundColor(R.color.def_page_bg_dark)

                setTitleBarVisibility(View.GONE)
            }

            PAGE_STYLE_PLIGHT_TITLE_SLIGHT_CONTENT -> {
                StatusBarMode(this, true)

                setTitleBackgroundColor(R.color.def_page_bg_light_primary)
                setContentBackgroundColor(R.color.def_page_bg_light_secondary)

                setTitleLeftImageResource(R.drawable.ic_back_dark)
                setTitleRightTextColor(R.color.def_text_font_black)
                titleColor = R.color.def_text_font_black
            }

            PAGE_STYLE_DARK_TITLE_PLIGHT_CONTENT -> {
                setTitleBackgroundResource(R.drawable.bg_title_dark)
                setContentBackgroundColor(R.color.def_page_bg_light_primary)

                setTitleLeftImageResource(R.drawable.ic_back_light)
                setTitleRightTextColor(R.color.white)
                titleColor = R.color.white
            }

            PAGE_STYLE_CUSTOM -> {
                setTitleBarVisibility(View.GONE)
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MultiLanguageUtility.attachBaseContext(newBase))
    }

    private fun StatusBarMode(activity: Activity, dark: Boolean): Int {
        var result = 0
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (MIUISetStatusBarLightMode(activity, dark)) {
                    result = 1
                } else if (FlymeSetStatusBarLightMode(activity.window, dark)) {
                    result = 2
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.window.decorView.systemUiVisibility =
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    result = 3
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun FlymeSetStatusBarLightMode(window: Window?, dark: Boolean): Boolean {
        var result = false
        if (window != null) {
            try {
                val lp = window.getAttributes()
                val darkFlag = WindowManager.LayoutParams::class.java
                    .getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON")
                val meizuFlags = WindowManager.LayoutParams::class.java
                    .getDeclaredField("meizuFlags")
                darkFlag.isAccessible = true
                meizuFlags.isAccessible = true
                val bit = darkFlag.getInt(null)
                var value = meizuFlags.getInt(lp)
                if (dark) {
                    value = value or bit
                } else {
                    value = value and bit.inv()
                }
                meizuFlags.setInt(lp, value)
                window.setAttributes(lp)
                result = true
            } catch (e: Exception) {

            }

        }
        return result
    }

    private fun MIUISetStatusBarLightMode(activity: Activity, dark: Boolean): Boolean {
        var result = false
        val window = activity.window
        if (window != null) {
            val clazz = window.javaClass
            try {
                var darkModeFlag = 0
                val layoutParams = Class.forName("android.view.MiuiWindowManager\$LayoutParams")
                val field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE")
                darkModeFlag = field.getInt(layoutParams)
                val extraFlagField = clazz.getMethod(
                    "setExtraFlags",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                if (dark) {
                    extraFlagField.invoke(window, darkModeFlag, darkModeFlag)//状态栏透明且黑色字体
                } else {
                    extraFlagField.invoke(window, 0, darkModeFlag)//清除黑色字体
                }
                result = true

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    //开发版 7.7.13 及以后版本采用了系统API，旧方法无效但不会报错，所以两个方式都要加上
                    if (dark) {
                        activity.window.decorView.systemUiVisibility =
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    } else {
                        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                    }
                }
            } catch (e: Exception) {

            }

        }
        return result
    }
}
