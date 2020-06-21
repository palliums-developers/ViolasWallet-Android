package com.violas.wallet.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.IntDef
import com.palliums.base.BaseActivity
import com.palliums.utils.StatusBarUtil
import com.palliums.utils.getResourceId
import com.violas.wallet.R
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility

abstract class BaseAppActivity : BaseActivity() {

    companion object {
        const val PAGE_STYLE_PRIMARY = 0
        const val PAGE_STYLE_SECONDARY = 1
        const val PAGE_STYLE_TERTIARY = 2
        const val PAGE_STYLE_NOT_TITLE = 3
        const val PAGE_STYLE_CUSTOM = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 布局延伸到状态栏中
        StatusBarUtil.layoutExtendsToStatusBar(window)
        super.onCreate(savedInstanceState)

        setTitleStyle(getTitleStyle())
    }

    @TitleStyle
    open fun getTitleStyle(): Int {
        return PAGE_STYLE_PRIMARY
    }

    @IntDef(
        PAGE_STYLE_PRIMARY,
        PAGE_STYLE_SECONDARY,
        PAGE_STYLE_TERTIARY,
        PAGE_STYLE_NOT_TITLE,
        PAGE_STYLE_CUSTOM
    )
    annotation class TitleStyle

    private fun setTitleStyle(@TitleStyle style: Int) {
        when (style) {
            PAGE_STYLE_PRIMARY -> {
                setTitleLeftImageResource(getResourceId(R.attr.iconBackPrimary, this))
                setTitleRightTextColor(getResourceId(android.R.attr.textColor, this))
                titleColor = getResourceId(android.R.attr.textColor, this)
            }

            PAGE_STYLE_SECONDARY -> {
                setTitleBackgroundColor(getResourceId(R.attr.colorSurface, this))
                setTitleLeftImageResource(getResourceId(R.attr.iconBackPrimary, this))
                setTitleRightTextColor(getResourceId(android.R.attr.textColor, this))
                titleColor = getResourceId(android.R.attr.textColor, this)
            }

            PAGE_STYLE_TERTIARY -> {
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
