package com.palliums.widget.status

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
            const val STATUS_FAILURE = 3        // 失败状态
            const val STATUS_NO_NETWORK = 4     // 无网络状态
        }
    }

    /**
     * 设置不同状态下的图片
     * @param status
     * @param imageRes
     */
    fun setImageWithStatus(@Status status: Int, @DrawableRes imageRes: Int)

    /**
     * 设置不同状态下的图片
     * @param status
     * @param image
     */
    fun setImageWithStatus(@Status status: Int, image: Drawable)

    /**
     * 设置不同状态下的提示文本
     * @param status
     * @param tipsRes
     */
    fun setTipsWithStatus(@Status status: Int, @StringRes tipsRes: Int)

    /**
     * 设置不同状态下的提示文本
     * @param status
     * @param tips
     */
    fun setTipsWithStatus(@Status status: Int, tips: String)

    /**
     * 显示状态
     * @param status
     */
    fun showStatus(@Status status: Int, errorMsg: String? = null)
}