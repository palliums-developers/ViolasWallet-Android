package com.palliums.paging

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Config
import androidx.paging.DataSource
import androidx.paging.PageKeyedDataSource
import androidx.paging.toLiveData
import com.palliums.net.LoadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

/**
 * Created by elephant on 2019-08-13 14:28.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: PagingViewModel
 */
abstract class PagingViewModel<VO> : ViewModel() {

    companion object {
        const val PAGE_SIZE = 10
    }

    private val result = MutableLiveData<PagingData<VO>>()
    val pagedList = Transformations.switchMap(result) { it.pagedList }
    val refreshState = Transformations.switchMap(result) { it.refreshState }
    val loadMoreState = Transformations.switchMap(result) { it.loadMoreState }
    val tipsMessage = Transformations.switchMap(result) { it.tipsMessage }

    /**
     * Just need to call once
     */
    fun start(pageSize: Int = PAGE_SIZE): Boolean {
        if (result.value != null) {
            return false
        }

        val realPageSize = if (pageSize < PAGE_SIZE / 2) PAGE_SIZE / 2 else pageSize
        val executor = ArchTaskExecutor.getIOThreadExecutor()
        val sourceFactory = PagingDataSourceFactory(executor, realPageSize)

        val listing: PagingData<VO> = PagingData(
            pagedList = sourceFactory.toLiveData(
                config = Config(
                    pageSize = realPageSize,
                    // 注意️ initialLoadSizeHint 必须是 pageSize 的2倍及以上整数倍，
                    // 否则会出现刷新操作之后 loadAfter 不回调的问题
                    initialLoadSizeHint = realPageSize * 2,
                    prefetchDistance = 1,
                    enablePlaceholders = false
                ),
                fetchExecutor = executor
            ),
            refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) { it.refreshState },
            loadMoreState = Transformations.switchMap(sourceFactory.sourceLiveData) { it.loadMoreState },
            tipsMessage = Transformations.switchMap(sourceFactory.sourceLiveData) { it.tipsMessage },
            refresh = { sourceFactory.sourceLiveData.value?.refresh() },
            retry = { sourceFactory.sourceLiveData.value?.retry() }
        )

        result.value = listing
        return true
    }

    fun refresh() {
        result.value?.refresh?.invoke()
    }

    fun retry() {
        result.value?.retry?.invoke()
    }

    /**
     * 加载数据
     * @param pageSize 分页大小，默认为10
     * @param pageNumber 页码，从1开始
     * @param pageKey 页面键，来源[onSuccess]返回的第二个数据，开始为null
     * @param onSuccess 成功回调
     * @param onFailure 失败回调
     */
    protected abstract suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<VO>, Any?) -> Unit,
        onFailure: (Throwable) -> Unit
    )

    inner class PagingDataSourceFactory(
        private val retryExecutor: Executor,
        private val pageSize: Int
    ) : DataSource.Factory<Int, VO>() {

        val sourceLiveData = MutableLiveData<PagingDataSource>()

        override fun create(): DataSource<Int, VO> {
            val source = PagingDataSource(retryExecutor, pageSize)

            sourceLiveData.postValue(source)

            return source
        }
    }

    inner class PagingDataSource(
        private val retryExecutor: Executor,
        private val pageSize: Int
    ) : PageKeyedDataSource<Int, VO>() {

        private val lock: Any = Any()

        // keep a function reference for the retry event
        private var retry: (() -> Any)? = null

        private var pageNumber = 1
        private var nextPageKey: Any? = null

        val refreshState = MutableLiveData<LoadState>()
        val loadMoreState = MutableLiveData<LoadState>()
        val tipsMessage = MutableLiveData<String>()

        fun refresh() {
            synchronized(lock) {
                invalidate()
            }
        }

        fun retry() {
            synchronized(lock) {
                val prevRetry = retry

                retry = null

                prevRetry?.let {
                    retryExecutor.execute { it.invoke() }
                }
            }
        }

        override fun loadInitial(
            params: LoadInitialParams<Int>,
            callback: LoadInitialCallback<Int, VO>
        ) {
            synchronized(lock) {
                val currentRefreshState = refreshState.value
                if (currentRefreshState != null
                    && currentRefreshState.status == LoadState.Status.RUNNING
                ) {
                    // 处于刷新中时，不需要再次刷新操作
                    return
                }

                val currentLoadMoreState = loadMoreState.value
                if (currentLoadMoreState != null
                    && currentLoadMoreState.status == LoadState.Status.RUNNING
                ) {
                    // 处于加载更多中时，不处理刷新操作
                    return
                }

                refreshState.postValue(LoadState.RUNNING)
            }

            this@PagingViewModel.viewModelScope.launch(Dispatchers.IO) {

                try {
                    loadData(params.requestedLoadSize, 1, null,

                        onSuccess = { listData, pageKey ->

                            synchronized(lock) {

                                retry = null
                                pageNumber = params.requestedLoadSize / pageSize + 1
                                nextPageKey = pageKey

                                callback.onResult(listData, 0, pageNumber)

                                when {
                                    listData.isEmpty() -> {
                                        refreshState.postValue(LoadState.SUCCESS_EMPTY)
                                        loadMoreState.postValue(LoadState.IDLE)
                                    }

                                    listData.size < params.requestedLoadSize -> {
                                        refreshState.postValue(LoadState.SUCCESS_NO_MORE)
                                        // 为了底部显示没有更多一行
                                        loadMoreState.postValue(LoadState.SUCCESS_NO_MORE)
                                    }

                                    else -> {
                                        refreshState.postValue(LoadState.SUCCESS)
                                        loadMoreState.postValue(LoadState.IDLE)
                                    }
                                }
                            }
                        },

                        onFailure = {
                            synchronized(lock) {
                                retry = { loadInitial(params, callback) }

                                refreshState.postValue(LoadState.failure(it))
                                tipsMessage.postValue(it.message)
                            }
                        })

                } catch (e: Exception) {
                    e.printStackTrace()

                    synchronized(lock) {
                        retry = { loadInitial(params, callback) }

                        refreshState.postValue(LoadState.failure(e))
                        tipsMessage.postValue(e.message)
                    }
                }
            }
        }

        override fun loadAfter(
            params: LoadParams<Int>,
            callback: LoadCallback<Int, VO>
        ) {
            synchronized(lock) {
                val currentRefreshState = refreshState.value
                if (currentRefreshState != null
                    && currentRefreshState.status != LoadState.Status.IDLE
                    && currentRefreshState.status != LoadState.Status.SUCCESS
                ) {
                    // 处于刷新中、刷新失败、刷新没有更多时，不处理加载更多操作
                    return
                }

                val currentLoadMoreState = loadMoreState.value
                if (currentLoadMoreState != null
                    && (currentLoadMoreState.status == LoadState.Status.RUNNING
                            || currentLoadMoreState.status == LoadState.Status.SUCCESS_NO_MORE)
                ) {
                    // 处于加载更多中时，不需要再次加载更多操作；处于加载更多没有更多时，不处理加载更多
                    return
                }

                loadMoreState.postValue(LoadState.RUNNING)
            }

            this@PagingViewModel.viewModelScope.launch(Dispatchers.IO) {

                try {
                    loadData(params.requestedLoadSize, pageNumber, nextPageKey,

                        onSuccess = { listData, pageKey ->

                            synchronized(lock) {

                                retry = null
                                pageNumber++
                                nextPageKey = pageKey

                                callback.onResult(listData, pageNumber)

                                loadMoreState.postValue(
                                    when {
                                        listData.size < params.requestedLoadSize ->
                                            LoadState.SUCCESS_NO_MORE

                                        else ->
                                            LoadState.SUCCESS
                                    }
                                )
                            }
                        },

                        onFailure = {
                            synchronized(lock) {
                                retry = { loadAfter(params, callback) }

                                loadMoreState.postValue(LoadState.failure(it))
                                tipsMessage.postValue(it.message)
                            }
                        })

                } catch (e: Exception) {
                    e.printStackTrace()

                    synchronized(lock) {
                        retry = { loadAfter(params, callback) }

                        loadMoreState.postValue(LoadState.failure(e))
                        tipsMessage.postValue(e.message)
                    }
                }
            }
        }

        override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, VO>) {
            // ignored, since we only ever append to our initial load
        }
    }
}