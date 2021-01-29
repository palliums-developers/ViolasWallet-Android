package com.palliums.paging

import androidx.lifecycle.*
import androidx.paging.Config
import androidx.paging.DataSource
import androidx.paging.PageKeyedDataSource
import androidx.paging.toLiveData
import com.palliums.extensions.getShowErrorMessage
import com.palliums.extensions.isActiveCancellation
import com.palliums.extensions.logError
import com.palliums.extensions.logInfo
import com.palliums.net.LoadState
import com.palliums.utils.coroutineExceptionHandler
import kotlinx.coroutines.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Created by elephant on 2019-08-13 14:28.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: PagingViewModel
 */
abstract class PagingViewModel<VO> : ViewModel() {

    companion object {
        private const val TAG = "PagingViewModel"

        const val PAGE_SIZE = 10
    }

    private val result = MutableLiveData<PagingData<VO>>()
    val pagedList = Transformations.switchMap(result) { it.pagedList }
    val refreshState = Transformations.switchMap(result) { it.refreshState }
    val loadMoreState = Transformations.switchMap(result) { it.loadMoreState }
    val pagingTipsMessage = Transformations.switchMap(result) { it.tipsMessage }

    /**
     * Just need to call once
     */
    fun start(pageSize: Int = PAGE_SIZE, fixedPageSize: Boolean = false): Boolean {
        if (result.value != null) {
            return false
        }

        val realPageSize = if (pageSize < PAGE_SIZE / 2) PAGE_SIZE / 2 else pageSize
        val executor = Executors.newSingleThreadExecutor()
        val sourceFactory = PagingDataSourceFactory(executor, realPageSize)

        val listing: PagingData<VO> = PagingData(
            pagedList = sourceFactory.toLiveData(
                config = Config(
                    pageSize = realPageSize,
                    // 注意️ initialLoadSizeHint 必须是 pageSize 的2倍及以上整数倍，
                    // 否则会出现刷新操作之后 loadAfter 不回调的问题
                    initialLoadSizeHint = if (fixedPageSize) realPageSize else realPageSize * 2,
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
     */
    protected abstract suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<VO>, Any?) -> Unit
    )

    inner class PagingDataSourceFactory(
        private val retryExecutor: Executor,
        private val pageSize: Int
    ) : DataSource.Factory<Int, VO>() {

        val sourceLiveData = MutableLiveData<PagingDataSource>()

        override fun create(): DataSource<Int, VO> {
            logInfo(TAG) { "create PagingDataSource" }
            sourceLiveData.value?.cancel()

            val source = PagingDataSource(retryExecutor, pageSize)

            sourceLiveData.postValue(source)

            return source
        }
    }

    inner class PagingDataSource(
        private val retryExecutor: Executor,
        private val pageSize: Int
    ) : PageKeyedDataSource<Int, VO>() {

        private val lock by lazy { Any() }

        // keep a function reference for the retry event
        private var retry: (() -> Any)? = null

        private var pageNumber = 1
        private var nextPageKey: Any? = null
        private var loadDataJob: Job? = null

        val refreshState by lazy { EnhancedMutableLiveData<LoadState>() }
        val loadMoreState by lazy { EnhancedMutableLiveData<LoadState>() }
        val tipsMessage by lazy { EnhancedMutableLiveData<String>() }

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

        fun cancel() {
            loadDataJob?.let {
                try {
                    it.cancel()
                } catch (ignore: Exception) {
                }
                loadDataJob = null
            }
        }

        override fun loadInitial(
            params: LoadInitialParams<Int>,
            callback: LoadInitialCallback<Int, VO>
        ) {
            logInfo(TAG) { "refresh data => start" }
            synchronized(lock) {
                val currentRefreshState = refreshState.value?.peekData()
                if (currentRefreshState?.status == LoadState.Status.RUNNING) {
                    // 处于刷新中时，不需要再次刷新操作
                    logInfo(TAG) { "refresh data => end" }
                    return
                }

                val currentLoadMoreState = loadMoreState.value?.peekData()
                if (currentLoadMoreState?.status == LoadState.Status.RUNNING) {
                    // 处于加载更多中时，不处理刷新操作
                    logInfo(TAG) { "refresh data => end" }
                    return
                }

                refreshState.postValueSupport(LoadState.RUNNING)
            }

            loadDataJob = this@PagingViewModel.viewModelScope
                .launch(Dispatchers.IO + coroutineExceptionHandler()) {

                    val startTime = System.currentTimeMillis()
                    try {
                        loadData(params.requestedLoadSize, 1, null,

                            onSuccess = { listData, pageKey ->

                                logInfo(TAG) { "refresh data => success" }

                                synchronized(lock) {

                                    retry = null
                                    pageNumber = params.requestedLoadSize / pageSize + 1
                                    nextPageKey = pageKey

                                    callback.onResult(listData, 0, pageNumber)

                                    when {
                                        listData.isEmpty() -> {
                                            refreshState.postValueSupport(LoadState.SUCCESS_EMPTY)
                                            loadMoreState.postValueSupport(LoadState.IDLE)
                                        }

                                        listData.size < params.requestedLoadSize -> {
                                            refreshState.postValueSupport(LoadState.SUCCESS_NO_MORE)
                                            // 为了底部显示没有更多一行
                                            loadMoreState.postValueSupport(LoadState.SUCCESS_NO_MORE)
                                        }

                                        else -> {
                                            refreshState.postValueSupport(LoadState.SUCCESS)
                                            loadMoreState.postValueSupport(LoadState.IDLE)
                                        }
                                    }
                                }
                            })

                    } catch (e: Exception) {
                        val activeCancellation = e.isActiveCancellation()
                        if (!activeCancellation) {
                            logError(e, TAG) { "refresh data => failure" }
                            delayOnError(startTime)
                        }

                        synchronized(lock) {
                            if (activeCancellation) {
                                refreshState.postValueSupport(LoadState.IDLE)
                            } else {
                                retry = { loadInitial(params, callback) }

                                refreshState.postValueSupport(LoadState.failure(e))
                                tipsMessage.postValueSupport(e.getShowErrorMessage(true))
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        loadDataJob = null
                    }
                }
        }

        override fun loadAfter(
            params: LoadParams<Int>,
            callback: LoadCallback<Int, VO>
        ) {
            logInfo(TAG) { "load more data => start" }
            synchronized(lock) {
                val currentRefreshState = refreshState.value?.peekData()
                if (currentRefreshState != null
                    && currentRefreshState.status != LoadState.Status.IDLE
                    && currentRefreshState.status != LoadState.Status.SUCCESS
                ) {
                    // 处于刷新中、刷新失败、刷新没有更多时，不处理加载更多操作
                    logInfo(TAG) { "load more data => end" }
                    return
                }

                val currentLoadMoreState = loadMoreState.value?.peekData()
                if (currentLoadMoreState != null
                    && (currentLoadMoreState.status == LoadState.Status.RUNNING
                            || currentLoadMoreState.status == LoadState.Status.SUCCESS_NO_MORE)
                ) {
                    // 处于加载更多中时，不需要再次加载更多操作；处于加载更多没有更多时，不处理加载更多
                    logInfo(TAG) { "load more data => end" }
                    return
                }

                loadMoreState.postValueSupport(LoadState.RUNNING)
            }

            loadDataJob = this@PagingViewModel.viewModelScope
                .launch(Dispatchers.IO + coroutineExceptionHandler()) {

                    val startTime = System.currentTimeMillis()
                    try {
                        loadData(params.requestedLoadSize, pageNumber, nextPageKey,

                            onSuccess = { listData, pageKey ->
                                logInfo(TAG) { "load more data => success" }

                                synchronized(lock) {

                                    retry = null
                                    pageNumber++
                                    nextPageKey = pageKey

                                    callback.onResult(listData, pageNumber)

                                    loadMoreState.postValueSupport(
                                        when {
                                            listData.size < params.requestedLoadSize ->
                                                LoadState.SUCCESS_NO_MORE

                                            else ->
                                                LoadState.SUCCESS
                                        }
                                    )
                                }
                            })

                    } catch (e: Exception) {
                        val activeCancellation = e.isActiveCancellation()
                        if (!activeCancellation) {
                            logError(e, TAG) { "load more data => failure" }
                            delayOnError(startTime)
                        }

                        synchronized(lock) {
                            if (activeCancellation) {
                                loadMoreState.postValueSupport(LoadState.IDLE)
                            } else {
                                retry = { loadAfter(params, callback) }

                                loadMoreState.postValueSupport(LoadState.failure(e))
                                tipsMessage.postValueSupport(e.getShowErrorMessage(true))
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        loadDataJob = null
                    }
                }
        }

        override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, VO>) {
            // ignored, since we only ever append to our initial load
        }

        private suspend fun delayOnError(startTime: Long) {
            val spentTime = System.currentTimeMillis() - startTime
            if (System.currentTimeMillis() - startTime < 1000) {
                try {
                    delay(1000 - spentTime)
                } catch (ignore: Exception) {
                }
            }
        }
    }
}