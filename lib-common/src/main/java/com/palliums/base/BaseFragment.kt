package com.palliums.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.palliums.utils.isFastMultiClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import me.yokeyword.fragmentation.SupportFragment

/**
 * Created by elephant on 2019-10-23 16:36.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
abstract class BaseFragment : SupportFragment(), View.OnClickListener,
    CoroutineScope by MainScope() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(getLayoutResId(), container, false)
    }

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

    fun showProgress(@StringRes resId: Int) {
        showProgress(getString(resId))
    }

    fun showProgress(msg: String? = null) {
        (_mActivity as? BaseActivity)?.showProgress(msg)
    }

    fun dismissProgress() {
        (_mActivity as? BaseActivity)?.dismissProgress()
    }

    fun showToast(@StringRes msgId: Int) {
        showToast(getString(msgId))
    }

    fun showToast(msg: String) {
        (_mActivity as? BaseActivity)?.showToast(msg)
    }

    fun finishActivity(){
        _mActivity.finish()
    }
}