package com.palliums.listing

import androidx.recyclerview.widget.RecyclerView
import com.palliums.widget.refresh.IRefreshLayout
import com.palliums.widget.status.IStatusLayout

/**
 * Created by elephant on 2019-11-06 14:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface ListingController<VO> {

    /**
     * 加载使用对话框，返回false则使用RefreshLayout的加载View
     */
    fun loadingUseDialog() = true

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
     * 获取ViewModel
     */
    fun getViewModel(): ListingViewModel<VO>

    /**
     * 获取ViewAdapter
     */
    fun getViewAdapter(): ListingViewAdapter<VO>
}