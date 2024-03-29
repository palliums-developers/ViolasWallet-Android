package com.palliums.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import com.palliums.utils.CustomMainScope
import com.palliums.utils.isFastMultiClick
import kotlinx.coroutines.CoroutineScope
import me.yokeyword.fragmentation.SupportFragment

/**
 * Created by elephant on 2019-10-23 16:36.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
abstract class BaseFragment : SupportFragment(), View.OnClickListener, ViewController,
    CoroutineScope by CustomMainScope() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(getLayoutResId(), container, false)
    }

    @LayoutRes
    abstract fun getLayoutResId(): Int

    /**
     * View点击回调，已防重点击处理
     * @param view
     */
    protected open fun onViewClick(view: View) {

    }

    /**
     * 防重点击处理，子类复写[onViewClick]来响应事件
     */
    final override fun onClick(view: View) {
        if (!isFastMultiClick(view)) {
            onViewClick(view)
        }
    }

    override fun showProgress(@StringRes resId: Int) {
        showProgress(getString(resId))
    }

    override fun showProgress(msg: String?) {
        (activity as? BaseActivity)?.showProgress(msg)
    }

    override fun dismissProgress() {
        (activity as? BaseActivity)?.dismissProgress()
    }

    override fun showToast(@StringRes msgId: Int) {
        showToast(getString(msgId))
    }

    override fun showToast(msg: String) {
        (activity as? BaseActivity)?.showToast(msg)
    }

    fun finishActivity() {
        activity?.finish()
    }

    fun setStatusBarMode(darkMode: Boolean) {
        (activity as? BaseActivity)?.setStatusBarMode( darkMode)
    }
}