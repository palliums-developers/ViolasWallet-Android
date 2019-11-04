package com.violas.wallet.paging

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.RecyclerView
import com.scwang.smartrefresh.layout.SmartRefreshLayout
import com.violas.wallet.repository.http.LoadState
import com.violas.wallet.widget.DataLoadStatusControl
import com.violas.wallet.widget.DataLoadStatusLayout

/**
 * Created by elephant on 2019-08-16 14:50.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: PagingRefreshHandler
 */
class PagingRefreshHandler<Vo>(
    lifecycleOwner: LifecycleOwner,
    recyclerView: RecyclerView,
    private val refreshLayout: SmartRefreshLayout,
    private val statusLayout: DataLoadStatusLayout?,
    private val pagingViewModel: PagingViewModel<Vo>,
    private val pagingAdapter: PagingAdapter<Vo>
) {

    private var updateData: Boolean = true
    private var newPagedList: PagedList<Vo>? = null

    init {
        recyclerView.adapter = pagingAdapter

        // 禁用上拉加载更多功能
        refreshLayout.setEnableLoadMore(false)
        // 启用越界回弹
        refreshLayout.setEnableOverScrollBounce(true)
        // 启用越界拖动
        refreshLayout.setEnableOverScrollDrag(true)
        refreshLayout.setOnRefreshListener {

            // 刷新时原有数据会被清空，造成短暂的页面闪屏或页面空白
            // 取消时时更新数据，刷新完成后再做处理
            updateData = false

            pagingViewModel.refresh()
        }

        statusLayout?.showStatus(DataLoadStatusControl.Status.STATUS_NONE)
        /*statusLayout?.onReloadListener = object : DataLoadStatusLayout.OnReloadListener {
            override fun onReload() {
                refreshLayout.setEnableRefresh(true)
                refreshLayout.autoRefreshAnimationOnly()
                pagingViewModel.refresh()
            }
        }*/

        pagingViewModel.pagedList.observe(lifecycleOwner, Observer {
            if (updateData) {
                pagingAdapter.submitList(it)
            } else {
                newPagedList = it
            }
        })

        pagingViewModel.refreshState.observe(lifecycleOwner, Observer {
            when (it.status) {
                LoadState.Status.RUNNING -> {
                    //statusLayout?.showStatus(DataLoadStatusControl.Status.STATUS_NONE)
                }

                LoadState.Status.SUCCESS -> {
                    handleRefreshDataUpdate(true)
                    refreshLayout.finishRefresh(true)
                    statusLayout?.showStatus(DataLoadStatusControl.Status.STATUS_NONE)
                }

                LoadState.Status.SUCCESS_EMPTY -> {
                    handleRefreshDataUpdate(true)
                    refreshLayout.finishRefreshWithNoMoreData()
                    //refreshLayout.setEnableRefresh(false)
                    statusLayout?.showStatus(DataLoadStatusControl.Status.STATUS_EMPTY)
                }

                LoadState.Status.SUCCESS_NO_MORE -> {
                    handleRefreshDataUpdate(true)
                    refreshLayout.finishRefreshWithNoMoreData()
                    statusLayout?.showStatus(DataLoadStatusControl.Status.STATUS_NONE)
                }

                LoadState.Status.FAILED -> {
                    handleRefreshDataUpdate(false)
                    refreshLayout.finishRefresh(false)
                    //refreshLayout.setEnableRefresh(false)
                    when {
                        pagingAdapter.itemCount > 0 ->
                            statusLayout?.showStatus(
                                DataLoadStatusControl.Status.STATUS_NONE
                            )
                        it.isNoNetwork() ->
                            statusLayout?.showStatus(
                                DataLoadStatusControl.Status.STATUS_NO_NETWORK, it.getErrorMsg()
                            )
                        else ->
                            statusLayout?.showStatus(
                                DataLoadStatusControl.Status.STATUS_FAIL, it.getErrorMsg()
                            )
                    }
                }
            }
        })

        pagingViewModel.loadMoreState.observe(lifecycleOwner, Observer {
            pagingAdapter.setLoadMoreState(it)
        })
    }

    private fun handleRefreshDataUpdate(refreshSuccess: Boolean) {
        if (updateData) {
            return
        }

        updateData = true
        newPagedList?.let {
            if (refreshSuccess) {
                pagingAdapter.submitList(it)
            }
            newPagedList = null
        }
    }

    fun start(pageSize: Int = PagingViewModel.PAGE_SIZE) {
        refreshLayout.autoRefreshAnimationOnly()
        if (!pagingViewModel.start(pageSize)) {
            pagingViewModel.refresh()
        }
    }
}