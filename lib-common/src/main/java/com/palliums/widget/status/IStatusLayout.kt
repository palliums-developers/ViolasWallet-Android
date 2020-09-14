package com.palliums.widget.status

import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.Px
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
        Status.STATUS_LOADING,
        Status.STATUS_EMPTY,
        Status.STATUS_FAILURE,
        Status.STATUS_NO_NETWORK
    )
    annotation class Status {
        companion object {
            const val STATUS_NONE = 1           // 默认状态，隐藏
            const val STATUS_LOADING = 2        // 加载状态
            const val STATUS_EMPTY = 3          // 空状态
            const val STATUS_FAILURE = 4        // 失败状态
            const val STATUS_NO_NETWORK = 5     // 无网络状态
        }
    }

    /**
     * 设置不同状态下的图片
     * @param status
     * @param imageRes
     */
    fun setImageWithStatus(@Status status: Int, @DrawableRes imageRes: Int): IStatusLayout

    /**
     * 设置不同状态下的图片
     * @param status
     * @param image
     */
    fun setImageWithStatus(@Status status: Int, image: Drawable): IStatusLayout

    /**
     * 设置不同状态下的提示文本
     * @param status
     * @param tipsRes
     */
    fun setTipsWithStatus(@Status status: Int, @StringRes tipsRes: Int): IStatusLayout

    /**
     * 设置不同状态下的提示文本
     * @param status
     * @param tips
     */
    fun setTipsWithStatus(@Status status: Int, tips: CharSequence): IStatusLayout

    /**
     * 显示状态
     * @param status
     */
    fun showStatus(@Status status: Int, errorMsg: CharSequence? = null)

    /**
     * 设置提示文本的字体大小
     */
    fun setTipsTextSize(@Px size: Int): IStatusLayout

    /**
     * 设置提示文本的字体颜色
     */
    fun setTipsTextColor(color: Int): IStatusLayout

    /**
     * 设置重试文本
     */
    fun setReloadText(@StringRes resId: Int): IStatusLayout

    /**
     * 设置重试文本
     */
    fun setReloadText(text: String): IStatusLayout

    /**
     * 设置重试文本的字体大小
     */
    fun setReloadTextSize(@Px size: Int): IStatusLayout

    /**
     * 设置重试文本的字体颜色
     */
    fun setReloadTextColor(color: Int): IStatusLayout

    /**
     * 获取重试控件
     */
    fun getReloadTextView(): TextView

    /**
     * 设置失败时显示重试
     */
    fun setShowReloadOnFailed(show: Boolean): IStatusLayout

    /**
     * 设置重试回调
     */
    fun setReloadCallback(callback: () -> Unit): IStatusLayout
}