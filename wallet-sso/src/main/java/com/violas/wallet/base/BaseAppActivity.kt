package com.violas.wallet.base

import android.content.Context
import android.os.Bundle
import android.view.View
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
                setStatusBarMode(true)

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
                setStatusBarMode(true)

                setTitleBackgroundColor(R.color.def_page_bg_light_primary)
                setContentBackgroundColor(R.color.def_page_bg_light_secondary)

                setTitleLeftImageResource(R.drawable.ic_back_dark)
                setTitleRightTextColor(R.color.def_text_font_black)
                titleColor = R.color.def_text_font_black
            }

            PAGE_STYLE_DARK_TITLE_PLIGHT_CONTENT -> {
                setTitleBackgroundResource(R.drawable.bg_title_black)
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
}
