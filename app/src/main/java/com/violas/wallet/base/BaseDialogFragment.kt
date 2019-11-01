package com.violas.wallet.base

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.violas.wallet.App
import com.violas.wallet.R

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

            it.setWindowAnimations(getWindowAnimationsStyle())
        }

        dialog?.setCancelable(cancelable())
        dialog?.setCanceledOnTouchOutside(canceledOnTouchOutside())

        super.onStart()
    }

    /**
     * 防重点击处理，子类复写[onViewClick]来响应事件
     */
    final override fun onClick(view: View) {
        if (!BaseActivity.isFastMultiClick(view)) {
            onViewClick(view)
        }
    }

    abstract fun getLayoutResId(): Int

    @StyleRes
    abstract fun getWindowAnimationsStyle(): Int

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
        (activity as? BaseActivity)?.showProgress(msg)
    }

    fun dismissProgress() {
        (activity as? BaseActivity)?.dismissProgress()
    }

    fun showToast(@StringRes msgId: Int) {
        showToast(getString(msgId))
    }

    fun showToast(msg: String) {
        (activity as? BaseActivity)?.showToast(msg)
    }
}