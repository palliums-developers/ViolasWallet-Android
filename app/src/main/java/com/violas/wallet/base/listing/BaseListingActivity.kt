package com.violas.wallet.base.listing

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.scwang.smartrefresh.layout.SmartRefreshLayout
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.widget.DataLoadStatusLayout
import kotlinx.android.synthetic.main.activity_base_listing.*

/**
 * Created by elephant on 2019-11-05 10:34.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 列表页面基类
 */
abstract class BaseListingActivity<VO> : BaseActivity() {

    protected val mViewModel by viewModels<BaseListingViewModel<VO>> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return lazyInitViewModel() as T
            }
        }
    }

    protected val mAdapter by lazy {
        lazyInitAdapter()
    }

    private val mHandler by lazy {
        ListingHandler(
            mAdapter,
            mViewModel,
            this,
            this,
            getRecyclerView(),
            getRefreshLayout(),
            getStatusLayout()
        )
    }

    /**
     * 子类通过覆写[getLayoutResId]返回自定义布局，必须包含RecyclerView
     */
    override fun getLayoutResId(): Int {
        return R.layout.activity_base_listing
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mHandler
    }

    /**
     * 子类覆写[getLayoutResId]后，需覆写[getRecyclerView]返回RecyclerView
     */
    protected open fun getRecyclerView(): RecyclerView {
        return vRecyclerView
    }

    /**
     * 子类覆写[getLayoutResId]后，需覆写[getRefreshLayout]返回SmartRefreshLayout
     */
    protected open fun getRefreshLayout(): SmartRefreshLayout? {
        return vRefreshLayout
    }

    /**
     * 子类覆写[getLayoutResId]后，需覆写[getStatusLayout]返回DataLoadStatusLayout
     */
    protected open fun getStatusLayout(): DataLoadStatusLayout? {
        return vStatusLayout
    }

    protected abstract fun lazyInitViewModel(): BaseListingViewModel<VO>

    protected abstract fun lazyInitAdapter(): BaseListingAdapter<VO>
}