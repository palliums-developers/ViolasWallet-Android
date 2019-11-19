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
        const val TITLE_STYLE_DEFAULT = 0
        const val TITLE_STYLE_GREY_BACKGROUND = 1
        const val TITLE_STYLE_MAIN_COLOR = 2
        const val TITLE_STYLE_NOT_TITLE = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitleStyle(getTitleStyle())
    }

    @TitleStyle
    open fun getTitleStyle(): Int {
        return TITLE_STYLE_DEFAULT
    }

    @IntDef(
        TITLE_STYLE_DEFAULT,
        TITLE_STYLE_GREY_BACKGROUND,
        TITLE_STYLE_MAIN_COLOR,
        TITLE_STYLE_NOT_TITLE
    )
    annotation class TitleStyle

    private fun setTitleStyle(@TitleStyle style: Int) {
        when (style) {

            TITLE_STYLE_GREY_BACKGROUND -> {
                StatusBarMode(this, true)

                setContentBackgroundColor(R.color.def_activity_vice_bg)
                setTitleBackgroundColor(R.color.white)

                setTitleLeftImageResource(R.drawable.icon_back_black)
                setTitleRightTextColor(R.color.def_text_font_black)
                titleColor = R.color.def_text_font_black
            }

            TITLE_STYLE_MAIN_COLOR -> {
                setRootBackgroundResource(R.drawable.shape_deputy_background)

                setTitleLeftImageResource(R.drawable.icon_back_white)
                setTitleRightTextColor(R.color.white)
                titleColor = R.color.white
            }

            TITLE_STYLE_NOT_TITLE -> {
                setRootBackgroundColor(R.color.def_activity_bg)

                setTitleBarVisibility(View.GONE)
            }

            else -> {
                StatusBarMode(this, true)

                setContentBackgroundColor(R.color.def_activity_bg)
                setTitleBackgroundColor(R.color.white)

                setTitleLeftImageResource(R.drawable.icon_back_black)
                setTitleRightTextColor(R.color.def_text_font_black)
                titleColor = R.color.def_text_font_black
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
