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
        const val TITLE_STYLE_DEFAULT = 0
        const val TITLE_STYLE_GREY_BACKGROUND = 1
        const val TITLE_STYLE_MAIN_COLOR = 2
        const val TITLE_STYLE_NOT_TITLE = 3
        const val TITLE_STYLE_DARK_TITLE_PLIGHT_CONTENT = 4
        const val TITLE_STYLE_CUSTOM = 5
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
        TITLE_STYLE_NOT_TITLE,
        /**
         * 深色title 主浅色content
         */
        TITLE_STYLE_DARK_TITLE_PLIGHT_CONTENT,
        TITLE_STYLE_CUSTOM
    )
    annotation class TitleStyle

    private fun setTitleStyle(@TitleStyle style: Int) {
        when (style) {

            TITLE_STYLE_GREY_BACKGROUND -> {
                setStatusBarMode(true)

                setContentBackgroundColor(R.color.def_activity_vice_bg)
                setTitleBackgroundColor(R.color.white)

                setTitleLeftImageResource(R.drawable.ic_back_dark)
                setTitleRightTextColor(R.color.def_text_font_black)
                titleColor = R.color.def_text_font_black
            }

            TITLE_STYLE_MAIN_COLOR -> {
                setRootBackgroundResource(R.drawable.shape_deputy_background)

                setTitleLeftImageResource(R.drawable.ic_back_light)
                setTitleRightTextColor(R.color.white)
                titleColor = R.color.white
            }

            TITLE_STYLE_NOT_TITLE -> {
                setRootBackgroundColor(R.color.def_activity_bg)

                setTitleBarVisibility(View.GONE)
            }

            TITLE_STYLE_DARK_TITLE_PLIGHT_CONTENT -> {
                setTopBackgroundResource(R.drawable.bg_wallet_main)
                setContentBackgroundColor(R.color.def_activity_bg)

                setTitleLeftImageResource(R.drawable.ic_back_light)
                setTitleRightTextColor(R.color.white)
                titleColor = R.color.white
            }

            TITLE_STYLE_DEFAULT -> {
                setStatusBarMode(true)

                setContentBackgroundColor(R.color.def_activity_bg)
                setTitleBackgroundColor(R.color.white)

                setTitleLeftImageResource(R.drawable.ic_back_dark)
                setTitleRightTextColor(R.color.def_text_font_black)
                titleColor = R.color.def_text_font_black
            }

            TITLE_STYLE_CUSTOM -> {

            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MultiLanguageUtility.attachBaseContext(newBase))
    }
}
