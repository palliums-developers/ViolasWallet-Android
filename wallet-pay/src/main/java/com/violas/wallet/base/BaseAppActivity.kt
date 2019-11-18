package com.violas.wallet.base

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.palliums.base.BaseActivity
import com.palliums.utils.StatusBarUtil
import com.palliums.utils.isFastMultiClick
import com.violas.wallet.R
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.layout_status_bar.*
import kotlinx.android.synthetic.main.layout_title_bar.*

abstract class BaseAppActivity : BaseActivity() {

    companion object {
        const val TITLE_STYLE_DEFAULT = 0
        const val TITLE_STYLE_GREY_BACKGROUND = 1
        const val TITLE_STYLE_MAIN_COLOR = 2
        const val TITLE_STYLE_NOT_TITLE = 3
    }

    abstract fun getLayoutResId(): Int
    protected open fun getLayoutView(): View? = null
    fun getRootView(): LinearLayout = vRootView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(com.palliums.R.layout.activity_base)

        val layoutView =
            getLayoutView() ?: layoutInflater.inflate(getLayoutResId(), vRootView, false)
        vRootView.addView(layoutView)

        vStatusBar.layoutParams.height = StatusBarUtil.getStatusBarHeight(this)

        vTitleLeftImageBtn?.setOnClickListener(this)

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
                vRootView.setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.def_activity_vice_bg,
                        null
                    )
                )
                StatusBarMode(this, true)
                vStatusBar.setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.white,
                        null
                    )
                )
                vTitleBar.setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.white,
                        null
                    )
                )
                vTitleLeftImageBtn.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.icon_back_black,
                        null
                    )
                )
                vTitleMiddleText.setTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.def_text_font_black,
                        null
                    )
                )
                vTitleRightTextBtn.setTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.def_text_font_black,
                        null
                    )
                )
            }
            TITLE_STYLE_MAIN_COLOR -> {
                vRootView.background = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.shape_deputy_background,
                    null
                )
                vStatusBar.setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.transparent,
                        null
                    )
                )
                vTitleBar.setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.transparent,
                        null
                    )
                )
                vTitleLeftImageBtn.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.icon_back_white,
                        null
                    )
                )
                vTitleMiddleText.setTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.white,
                        null
                    )
                )
                vTitleRightTextBtn.setTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.white,
                        null
                    )
                )
            }
            TITLE_STYLE_NOT_TITLE -> {
                vRootView.setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.def_activity_bg,
                        null
                    )
                )
                vStatusBar.visibility = View.GONE
                vTitleBar.visibility = View.GONE
            }
            else -> {
                vRootView.setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.def_activity_bg,
                        null
                    )
                )
                StatusBarMode(this, true)
                vStatusBar.setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.white,
                        null
                    )
                )
                vTitleBar.setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.white,
                        null
                    )
                )
                vTitleLeftImageBtn.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.icon_back_black,
                        null
                    )
                )
                vTitleMiddleText.setTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.def_text_font_black,
                        null
                    )
                )
                vTitleRightTextBtn.setTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.def_text_font_black,
                        null
                    )
                )
            }
        }
    }

    /**
     * 设置标题
     */
    override fun setTitle(@StringRes strId: Int) {
        if (vTitleMiddleText != null && strId != 0) {
            vTitleMiddleText.setText(strId)
        }
    }

    override fun setTitle(title: CharSequence?) {
        if (vTitleMiddleText != null && !title.isNullOrEmpty()) {
            vTitleMiddleText.text = title
        }
    }

    fun setTitleRightText(@StringRes strId: Int) {
        if (vTitleRightTextBtn != null && strId != 0) {
            vTitleRightTextBtn.setText(strId)
            vTitleRightTextBtn.visibility = View.VISIBLE
            vTitleRightTextBtn.setOnClickListener(this)
        } else if (vTitleRightTextBtn != null) {
            vTitleRightTextBtn.visibility = View.GONE
        }
    }

    fun setTitleRightImage(@DrawableRes resId: Int) {
        if (vTitleRightImageBtn != null && resId != 0) {
            vTitleRightImageBtn.setImageResource(resId)
            vTitleRightImageBtn.visibility = View.VISIBLE
            vTitleRightImageBtn.setOnClickListener(this)
        } else if (vTitleRightImageBtn != null) {
            vTitleRightImageBtn.visibility = View.GONE
        }
    }

    /**
     * 防重点击处理，子类复写[onViewClick]来响应事件
     */
    final override fun onClick(view: View) {
        if (!isFastMultiClick(view)) {
            // TitleBar的View点击事件与页面其它的View点击事件分开处理
            when (view.id) {
                com.palliums.R.id.vTitleLeftImageBtn ->
                    onTitleLeftViewClick()

                com.palliums.R.id.vTitleRightTextBtn,
                com.palliums.R.id.vTitleRightImageBtn ->
                    onTitleRightViewClick()

                else ->
                    onViewClick(view)
            }
        }
    }

    /**
     * TitleBar的左侧View点击回调，已防重点击处理
     * 默认关闭当前页面
     */
    protected open fun onTitleLeftViewClick() {
        finish()
    }

    /**
     * TitleBar的右侧View点击回调，已防重点击处理
     * 没有响应逻辑
     */
    protected open fun onTitleRightViewClick() {

    }

    /**
     * View点击回调，已防重点击处理
     * 该回调只会分发页面非TitleBar的View点击事件
     * 如需处理TitleBar的View点击事件，请按需覆写[onTitleLeftViewClick]和[onTitleRightViewClick]
     * @param view
     */
    protected open fun onViewClick(view: View) {

    }

    /**
     * 浅色状态模式，设置字体为深色
     */
    protected fun setLightStatusBar(isLightStatusBar: Boolean) {
        StatusBarUtil.setLightStatusBarMode(this, isLightStatusBar)
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
