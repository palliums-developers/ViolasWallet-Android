package com.violas.wallet.widget.status

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.StringRes

/**
 * Created by elephant on 2019-08-02 10:11.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 状态布局
 */
interface IStatusLayout {

    @IntDef(
        Status.STATUS_NONE,
        Status.STATUS_EMPTY,
        Status.STATUS_FAILURE,
        Status.STATUS_NO_NETWORK
    )
    annotation class Status {
        companion object {
            const val STATUS_NONE = 1           // 默认状态，隐藏
            const val STATUS_EMPTY = 2          // 空状态
            const val STATUS_FAILURE = 3           // 失败状态
            const val STATUS_NO_NETWORK = 4     // 无网络状态
        }
    }

    /**
     * 设置不同状态下的图片icon
     * @param status
     * @param iconRes
     */
    fun setImageWithStatus(@Status status: Int, @DrawableRes iconRes: Int)

    /**
     * 设置不同状态下的图片icon
     * @param status
     * @param icon
     */
    fun setImageWithStatus(@Status status: Int, icon: Drawable)

    /**
     * 设置不同状态下的提示文本
     * @param status
     * @param tipRes
     */
    fun setTipWithStatus(@Status status: Int, @StringRes tipRes: Int)

    /**
     * 设置不同状态下的提示文本
     * @param status
     * @param tip
     */
    fun setTipWithStatus(@Status status: Int, tip: String)

    /**
     * 显示状态
     * @param status
     */
    fun showStatus(@Status status: Int, errorMsg: String? = null)
}