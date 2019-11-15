package com.violas.wallet.base.paging

import androidx.recyclerview.widget.RecyclerView
import com.violas.wallet.widget.refresh.IRefreshLayout
import com.violas.wallet.widget.status.IStatusLayout

/**
 * Created by elephant on 2019-11-06 14:54.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface PagingController<VO> {

    /**
     * 获取RecyclerView
     */
    fun getRecyclerView(): RecyclerView

    /**
     * 获取RefreshLayout
     */
    fun getRefreshLayout(): IRefreshLayout?

    /**
     * 获取StatusLayout
     */
    fun getStatusLayout(): IStatusLayout?

    /**
     * 初始化ViewModel
     */
    fun initViewModel(): PagingViewModel<VO>

    /**
     * 初始化ViewAdapter
     */
    fun initViewAdapter(): PagingViewAdapter<VO>

    /**
     * 获取ViewModel
     */
    fun getViewModel(): PagingViewModel<VO>

    /**
     * 获取ViewAdapter
     */
    fun getViewAdapter(): PagingViewAdapter<VO>
}