package com.palliums.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentPagerAdapter
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

    private var lazyInitTag = false
    private var savedInstanceState: Bundle? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        lazyInitTag = false
        this.savedInstanceState = savedInstanceState
        return inflater.inflate(getLayoutResId(), container, false)
    }

    override fun onResume() {
        if (!lazyInitTag) {
            lazyInitTag = true
            onLazyInitViewByResume(savedInstanceState)
            savedInstanceState = null
        }
        super.onResume()
    }

    override fun onDestroyView() {
        lazyInitTag = false
        savedInstanceState = null
        super.onDestroyView()
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
     * 懒初始化，必须使用[FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT]才有效
     */
    protected open fun onLazyInitViewByResume(savedInstanceState: Bundle?) {

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
        (activity as? ViewController)?.showProgress(msg)
    }

    override fun dismissProgress() {
        (activity as? ViewController)?.dismissProgress()
    }

    override fun showToast(@StringRes msgId: Int, duration: Int) {
        showToast(getString(msgId), duration)
    }

    override fun showToast(msg: String, duration: Int) {
        (activity as? ViewController)?.showToast(msg, duration)
    }

    fun close() {
        (activity as? BaseActivity)?.close()
    }

    fun finishActivity() {
        activity?.finish()
    }

    fun setStatusBarMode(darkMode: Boolean) {
        (activity as? BaseActivity)?.setStatusBarMode(darkMode)
    }
}