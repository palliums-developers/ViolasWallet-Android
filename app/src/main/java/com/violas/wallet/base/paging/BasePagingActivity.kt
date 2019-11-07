package com.violas.wallet.base.paging

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.widget.refresh.IRefreshLayout
import com.violas.wallet.widget.status.IStatusLayout
import kotlinx.android.synthetic.main.activity_base_list.*

/**
 * Created by elephant on 2019-11-06 14:56.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 分页列表页面基类
 */
abstract class BasePagingActivity<VO> : BaseActivity(), PagingController<VO> {

    private val mViewModel by viewModels<PagingViewModel<VO>> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return initViewModel() as T
            }
        }
    }

    private val mViewAdapter by lazy {
        initViewAdapter()
    }

    protected val mPagingHandler by lazy {
        PagingHandler(this, this, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    /**
     * 子类通过覆写[getLayoutResId]返回自定义布局，必须包含RecyclerView
     */
    override fun getLayoutResId(): Int {
        return R.layout.activity_base_list
    }

    /**
     * 子类覆写[getLayoutResId]后，需覆写[getRecyclerView]返回RecyclerView
     */
    override fun getRecyclerView(): RecyclerView {
        return vRecyclerView
    }

    /**
     * 子类覆写[getLayoutResId]后，需覆写[getRefreshLayout]返回SmartRefreshLayout
     */
    override fun getRefreshLayout(): IRefreshLayout? {
        return vRefreshLayout
    }

    /**
     * 子类覆写[getLayoutResId]后，需覆写[getStatusLayout]返回DataLoadStatusLayout
     */
    override fun getStatusLayout(): IStatusLayout? {
        return vStatusLayout
    }

    final override fun getViewAdapter(): PagingViewAdapter<VO> {
        return mViewAdapter
    }

    final override fun getViewModel(): PagingViewModel<VO> {
        return mViewModel
    }
}