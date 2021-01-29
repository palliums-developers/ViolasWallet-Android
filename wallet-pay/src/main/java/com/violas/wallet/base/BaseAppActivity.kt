package com.violas.wallet.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.IntDef
import com.palliums.base.BaseActivity
import com.palliums.utils.getColorByAttrId
import com.palliums.utils.getResourceId
import com.palliums.utils.setSystemBar
import com.violas.wallet.R
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility

abstract class BaseAppActivity : BaseActivity() {

    companion object {
        const val PAGE_STYLE_LIGHT_MODE = 0
        const val PAGE_STYLE_LIGHT_MODE_PRIMARY_TOP_BAR = 1
        const val PAGE_STYLE_LIGHT_MODE_PRIMARY_NAV_BAR = 2
        const val PAGE_STYLE_DARK_MODE = 3
        const val PAGE_STYLE_NOT_TITLE = 4
        const val PAGE_STYLE_CUSTOM = 5
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setSystemBar(getTitleStyle())
        super.onCreate(savedInstanceState)
        setTitleStyle(getTitleStyle())
    }

    @TitleStyle
    open fun getTitleStyle(): Int {
        return PAGE_STYLE_LIGHT_MODE
    }

    @IntDef(
        PAGE_STYLE_LIGHT_MODE,
        PAGE_STYLE_LIGHT_MODE_PRIMARY_TOP_BAR,
        PAGE_STYLE_LIGHT_MODE_PRIMARY_NAV_BAR,
        PAGE_STYLE_DARK_MODE,
        PAGE_STYLE_NOT_TITLE,
        PAGE_STYLE_CUSTOM
    )
    annotation class TitleStyle

    private fun setSystemBar(@TitleStyle style: Int) {
        if (style == PAGE_STYLE_LIGHT_MODE || style == PAGE_STYLE_LIGHT_MODE_PRIMARY_TOP_BAR) {
            window.setSystemBar(lightModeStatusBar = true, lightModeNavigationBar = true)
        } else if (style == PAGE_STYLE_LIGHT_MODE_PRIMARY_NAV_BAR) {
            window.setSystemBar(
                lightModeStatusBar = true,
                lightModeNavigationBar = true,
                navigationBarColorAboveO = getColorByAttrId(R.attr.colorSurface, this)
            )
        } else if (style == PAGE_STYLE_DARK_MODE) {
            window.setSystemBar(lightModeStatusBar = false, lightModeNavigationBar = false)
        }
    }

    private fun setTitleStyle(@TitleStyle style: Int) {
        when (style) {
            PAGE_STYLE_LIGHT_MODE,
            PAGE_STYLE_LIGHT_MODE_PRIMARY_TOP_BAR,
            PAGE_STYLE_LIGHT_MODE_PRIMARY_NAV_BAR -> {
                setTitleLeftImageResource(getResourceId(R.attr.iconBackPrimary, this))
                setTitleRightTextColor(getResourceId(android.R.attr.textColor, this))
                titleColor = getResourceId(android.R.attr.textColor, this)
                if (style == PAGE_STYLE_LIGHT_MODE_PRIMARY_TOP_BAR) {
                    setTitleBackgroundColor(getResourceId(R.attr.colorSurface, this))
                }
            }

            PAGE_STYLE_DARK_MODE -> {
                setTitleLeftImageResource(getResourceId(R.attr.iconBackSecondary, this))
                setTitleRightTextColor(getResourceId(R.attr.colorOnPrimary, this))
                titleColor = getResourceId(R.attr.colorOnPrimary, this)
            }

            PAGE_STYLE_NOT_TITLE -> {
                setTitleBarVisibility(View.GONE)
            }

            PAGE_STYLE_CUSTOM -> {

            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MultiLanguageUtility.attachBaseContext(newBase))
    }
}
