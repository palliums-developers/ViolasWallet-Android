package com.palliums.paging

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import com.palliums.base.ViewController
import com.palliums.net.LoadState
import com.palliums.widget.status.IStatusLayout

/**
 * Created by elephant on 2019-08-16 14:50.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: PagingHandler
 */
class PagingHandler<VO>(
    mLifecycleOwner: LifecycleOwner,
    val mViewController: ViewController,
    val mPagingController: PagingController<VO>
) {

    /**
     * 更新数据标志，刷新中会置为false，刷新完成后再置为true
     */
    private var updateDataFlag: Boolean = true
    /**
     * [updateDataFlag]为false期间缓存的刷新数据
     */
    private var cachePagedList: PagedList<VO>? = null

    private var autoRefresh = false
    private var pageSize = PagingViewModel.PAGE_SIZE

    init {

        mPagingController.getViewModel().pagedList.observe(mLifecycleOwner, Observer {
            if (updateDataFlag) {
                mPagingController.getViewAdapter().submitList(it)
            } else {
                cachePagedList = it
            }
        })

        mPagingController.getViewModel().refreshState.observe(mLifecycleOwner, Observer {
            when (it.status) {
                LoadState.Status.RUNNING -> {
                    //mPagingController.getStatusLayout()?.showStatus(IStatusLayout.Status.STATUS_NONE)
                }

                LoadState.Status.SUCCESS,
                LoadState.Status.SUCCESS_NO_MORE -> {
                    handleRefreshDataUpdate(true)
                    mPagingController.getRefreshLayout()?.finishRefresh(true)
                    mPagingController.getStatusLayout()?.showStatus(
                        IStatusLayout.Status.STATUS_NONE
                    )
                }

                LoadState.Status.SUCCESS_EMPTY -> {
                    handleRefreshDataUpdate(true)
                    mPagingController.getRefreshLayout()?.finishRefresh(true)
                    //mPagingController.getRefreshLayout()?.setEnableRefresh(false)
                    mPagingController.getStatusLayout()?.showStatus(
                        IStatusLayout.Status.STATUS_EMPTY
                    )
                }

                LoadState.Status.FAILURE -> {
                    handleRefreshDataUpdate(false)
                    mPagingController.getRefreshLayout()?.finishRefresh(
                        300, false, false
                    )
                    //mPagingController.getRefreshLayout()?.setEnableRefresh(false)
                    when {
                        mPagingController.getViewAdapter().itemCount > 0 ->
                            mPagingController.getStatusLayout()?.showStatus(
                                IStatusLayout.Status.STATUS_NONE
                            )

                        it.isNoNetwork() ->
                            mPagingController.getStatusLayout()?.showStatus(
                                IStatusLayout.Status.STATUS_NO_NETWORK, it.getErrorMsg()
                            )

                        else ->
                            mPagingController.getStatusLayout()?.showStatus(
                                IStatusLayout.Status.STATUS_FAILURE, it.getErrorMsg()
                            )
                    }
                }
            }
        })

        mPagingController.getViewModel().loadMoreState.observe(mLifecycleOwner, Observer {
            mPagingController.getViewAdapter().setLoadMoreState(it)
        })

        mPagingController.getViewModel().pagingTipsMessage.observe(mLifecycleOwner, Observer {
            if (it.isNotEmpty()) {
                mViewController.showToast(it)
            }
        })

        mPagingController.getRefreshLayout()?.let {
            it.setEnableLoadMore(false)         // 禁用上拉加载更多功能
            it.setEnableOverScrollBounce(true)  // 启用越界回弹
            it.setEnableOverScrollDrag(true)    // 启用越界拖动
            it.setOnRefreshListener {
                if (autoRefresh) {
                    autoRefresh = false
                    if (!mPagingController.getViewModel().start(pageSize)) {
                        mPagingController.getViewModel().refresh()
                    }
                } else {
                    // 刷新时原有数据会被清空，造成短暂的页面闪屏或页面空白
                    // 刷新时先不更新数据，刷新完成后再做处理
                    updateDataFlag = false

                    mPagingController.getViewModel().refresh()
                }
            }
        }

        mPagingController.getStatusLayout()?.showStatus(IStatusLayout.Status.STATUS_NONE)
        /*mPagingController.getStatusLayout()?.onReloadListener = object : DefaultStatusLayout.OnReloadListener {
            override fun onReload() {
                mPagingController.getRefreshLayout()?.setEnableRefresh(true)
                mPagingController.getRefreshLayout()?.autoRefreshAnimationOnly()
                mPagingController.getViewModel().retry()
            }
        }*/

        mPagingController.getRecyclerView().adapter = mPagingController.getViewAdapter()
    }

    private fun handleRefreshDataUpdate(refreshSuccess: Boolean) {
        if (updateDataFlag) {
            return
        }

        updateDataFlag = true
        cachePagedList?.let {
            if (refreshSuccess) {
                mPagingController.getViewAdapter().submitList(it)
            }
            cachePagedList = null
        }
    }

    fun start(pageSize: Int = PagingViewModel.PAGE_SIZE) {
        this.pageSize = pageSize
        autoRefresh = true
        mPagingController.getRefreshLayout()?.autoRefresh()

        // 使用下面的方式初始化刷新加载，SmartRefreshLayout第一次下滑会出现onRefresh刷新回调
        /*mPagingController.getRefreshLayout()?.autoRefreshAnimationOnly()
        if (!mPagingController.getViewModel().start(pageSize)) {
            mPagingController.getViewModel().refresh()
        }*/
    }
}