package com.palliums.paging

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import com.palliums.base.ViewController
import com.palliums.extensions.logInfo
import com.palliums.net.LoadState
import com.palliums.widget.status.IStatusLayout
import com.scwang.smartrefresh.layout.constant.RefreshState

/**
 * Created by elephant on 2019-08-16 14:50.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: PagingHandler
 */
class PagingHandler<VO>(
    private val mLifecycleOwner: LifecycleOwner,
    private val mViewController: ViewController,
    private val mPagingController: PagingController<VO>
) {

    companion object {
        private const val TAG = "PagingHandler"
    }

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
    private var fixedPageSize = false

    fun init() {
        mPagingController.getPagingViewModel().pagedList.observe(mLifecycleOwner) {
            logInfo(TAG) {
                "pagedList onChanged, updateDataFlag = $updateDataFlag, $it"
            }
            if (updateDataFlag) {
                mPagingController.getPagingViewAdapter().submitList(it)
            } else {
                cachePagedList = it
            }
        }

        mPagingController.getPagingViewModel().refreshState.observe(mLifecycleOwner) {
            logInfo(TAG) {
                "refreshState onChanged, updateDataFlag = $updateDataFlag, ${it.peekData()}"
            }
            when (it.peekData().status) {
                LoadState.Status.RUNNING -> {
                    //mPagingController.getStatusLayout()?.showStatus(IStatusLayout.Status.STATUS_NONE)
                }

                LoadState.Status.SUCCESS,
                LoadState.Status.SUCCESS_NO_MORE -> {
                    handleRefreshDataUpdate(true)
                    mPagingController.getRefreshLayout()?.finishRefresh(true)
                    mPagingController.getRefreshLayout()?.setEnableRefresh(true)
                    mPagingController.getStatusLayout()?.showStatus(
                        IStatusLayout.Status.STATUS_NONE
                    )
                }

                LoadState.Status.SUCCESS_EMPTY -> {
                    handleRefreshDataUpdate(true)
                    mPagingController.getRefreshLayout()?.finishRefresh(true)
                    mPagingController.getRefreshLayout()?.setEnableRefresh(true)
                    mPagingController.getStatusLayout()?.showStatus(
                        IStatusLayout.Status.STATUS_EMPTY
                    )
                }

                LoadState.Status.FAILURE -> {
                    handleRefreshDataUpdate(false)
                    mPagingController.getRefreshLayout()?.finishRefresh(
                        300, false, false
                    )
                    mPagingController.getRefreshLayout()?.setEnableRefresh(true)
                    when {
                        mPagingController.getPagingViewAdapter().itemCount > 0 ->
                            mPagingController.getStatusLayout()?.showStatus(
                                IStatusLayout.Status.STATUS_NONE
                            )

                        it.peekData().isNoNetwork() ->
                            mPagingController.getStatusLayout()?.showStatus(
                                IStatusLayout.Status.STATUS_NO_NETWORK, it.peekData().getErrorMsg()
                            )

                        else ->
                            mPagingController.getStatusLayout()?.showStatus(
                                IStatusLayout.Status.STATUS_FAILURE, it.peekData().getErrorMsg()
                            )
                    }
                }
            }
        }

        mPagingController.getPagingViewModel().loadMoreState.observe(mLifecycleOwner) {
            logInfo(TAG) {
                "loadMoreState onChanged, updateDataFlag = $updateDataFlag, ${it.peekData()}"
            }
            mPagingController.getPagingViewAdapter().setLoadMoreState(it.peekData())
        }

        mPagingController.getPagingViewModel().pagingTipsMessage.observe(mLifecycleOwner) {
            it.getDataIfNotHandled()?.let { msg ->
                if (msg.isNotEmpty()) {
                    mViewController.showToast(msg)
                }
            }
        }

        mPagingController.getRefreshLayout()?.let {
            it.setEnableRefresh(false)          // 首次加载使用[IStatusLayout]的加载效果，要禁用下拉刷新
            it.setEnableLoadMore(false)         // 禁用上拉加载更多功能
            it.setEnableOverScrollDrag(true)    // 启用越界拖动
            it.setEnableOverScrollBounce(false) // 关闭越界回弹
            it.setOnRefreshListener {
                logInfo(TAG) { "onRefresh" }
                if (autoRefresh) {
                    autoRefresh = false
                    if (!mPagingController.getPagingViewModel().start(pageSize, fixedPageSize)) {
                        mPagingController.getPagingViewModel().refresh()
                    }
                } else {
                    // 刷新时原有数据会被清空，造成短暂的页面闪屏或页面空白
                    // 刷新时先不更新数据，刷新完成后再做处理
                    updateDataFlag = false

                    mPagingController.getPagingViewModel().refresh()
                }
            }
        }

        /*mPagingController.getStatusLayout()?.setReloadCallback {
            mPagingController.getStatusLayout()?.showStatus(IStatusLayout.Status.STATUS_LOADING)
            mPagingController.getViewModel().retry()
        }*/
        mPagingController.getStatusLayout()?.showStatus(IStatusLayout.Status.STATUS_LOADING)

        mPagingController.getPagingViewAdapter().setRetryCallback {
            mPagingController.getPagingViewModel().retry()
        }

        mPagingController.getRecyclerView().adapter = mPagingController.getPagingViewAdapter()
    }

    private fun handleRefreshDataUpdate(refreshSuccess: Boolean) {
        if (updateDataFlag) {
            return
        }

        updateDataFlag = true
        cachePagedList?.let {
            if (refreshSuccess) {
                mPagingController.getPagingViewAdapter().submitList(it)
            }
            cachePagedList = null
        }
    }

    fun restart() {
        logInfo(TAG) { "restart" }
        // 清除加载更多和下拉刷新动画
        mPagingController.getPagingViewAdapter().setLoadMoreState(LoadState.IDLE)
        mPagingController.getRefreshLayout()?.let {
            if (it.state == RefreshState.Refreshing) {
                it.finishRefresh()
            }
            it.setEnableRefresh(false)
        }

        // 重新初始加载
        mPagingController.getStatusLayout()?.showStatus(IStatusLayout.Status.STATUS_LOADING)
        updateDataFlag = true
        mPagingController.getPagingViewModel().refresh()
    }

    fun start(pageSize: Int = PagingViewModel.PAGE_SIZE, fixedPageSize: Boolean = false) {
        logInfo(TAG) { "start" }
        this.pageSize = pageSize
        this.fixedPageSize = fixedPageSize
        autoRefresh = false
        //mPagingController.getRefreshLayout()?.autoRefresh()
        if (!mPagingController.getPagingViewModel().start(pageSize, fixedPageSize)) {
            mPagingController.getPagingViewModel().refresh()
        }

        // 使用下面的方式初始化刷新加载，SmartRefreshLayout第一次下滑会出现onRefresh刷新回调
        /*mPagingController.getRefreshLayout()?.autoRefreshAnimationOnly()
        if (!mPagingController.getViewModel().start(pageSize)) {
            mPagingController.getViewModel().refresh()
        }*/
    }
}