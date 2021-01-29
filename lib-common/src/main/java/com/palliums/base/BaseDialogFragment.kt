package com.palliums.base

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.palliums.R
import com.palliums.content.App
import com.palliums.utils.isFastMultiClick

/**
 * Created by elephant on 2019-11-01 14:21.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
abstract class BaseDialogFragment : DialogFragment(), View.OnClickListener {

    init {
        setStyle(STYLE_NORMAL, R.style.ThemeDefaultDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(getLayoutResId(), container, false)
    }

    override fun onStart() {
        dialog?.window?.let {
            val displayMetrics = DisplayMetrics()
            activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)

            val attributes = it.attributes
            attributes.width = WindowManager.LayoutParams.MATCH_PARENT
            attributes.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes.gravity = getWindowLayoutParamsGravity()
            it.attributes = attributes

            it.setWindowAnimations(getWindowAnimationsStyleId())
        }

        dialog?.setCancelable(cancelable())
        dialog?.setCanceledOnTouchOutside(canceledOnTouchOutside())

        super.onStart()
    }

    /**
     * 防重点击处理，子类复写[onViewClick]来响应事件
     */
    final override fun onClick(view: View) {
        if (!isFastMultiClick(view)) {
            onViewClick(view)
        }
    }

    /**
     * 获取弹窗布局资源id
     */
    @LayoutRes
    abstract fun getLayoutResId(): Int

    /**
     * 获取窗口动画样式资源id，返回的动画样式要与[getWindowLayoutParamsGravity]返回的对齐方式相协调
     */
    @StyleRes
    abstract fun getWindowAnimationsStyleId(): Int

    /**
     * 获取窗口布局对齐方式，返回的对齐方式要与[getWindowAnimationsStyleId]返回的动画样式相协调
     */
    abstract fun getWindowLayoutParamsGravity(): Int

    open fun cancelable(): Boolean = true

    open fun canceledOnTouchOutside(): Boolean = true

    /**
     * View点击回调，已防重点击处理
     * @param view
     */
    protected open fun onViewClick(view: View) {

    }

    fun show(tag: String = this.javaClass.name) {
        val activity = App.activityStore.peek()
        if (activity is FragmentActivity) {
            show(activity.supportFragmentManager, tag)
        }
    }

    fun close() {
        if (!isDetached && !isRemoving && fragmentManager != null) {
            dismissAllowingStateLoss()
        }
    }

    fun showProgress(@StringRes resId: Int) {
        showProgress(getString(resId))
    }

    fun showProgress(msg: String? = null) {
        (activity as? ViewController)?.showProgress(msg)
    }

    fun dismissProgress() {
        (activity as? ViewController)?.dismissProgress()
    }

    fun showToast(@StringRes msgId: Int) {
        showToast(getString(msgId))
    }

    fun showToast(msg: String) {
        (activity as? ViewController)?.showToast(msg)
    }
}