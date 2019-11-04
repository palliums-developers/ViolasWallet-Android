package com.violas.wallet.paging

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Config
import androidx.paging.DataSource
import androidx.paging.PageKeyedDataSource
import androidx.paging.toLiveData
import com.violas.wallet.repository.http.LoadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

/**
 * Created by elephant on 2019-08-13 14:28.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: PagingViewModel
 */
abstract class PagingViewModel<Vo> : ViewModel() {

    companion object {
        const val PAGE_SIZE = 10
    }

    private val result = MutableLiveData<Listing<Vo>>()
    val pagedList = Transformations.switchMap(result) { it.pagedList }
    val refreshState = Transformations.switchMap(result) { it.refreshState }
    val loadMoreState = Transformations.switchMap(result) { it.loadMoreState }

    /**
     * Just need to call once
     */
    fun start(pageSize: Int = PAGE_SIZE): Boolean {
        if (result.value != null) {
            return false
        }

        val executor = ArchTaskExecutor.getIOThreadExecutor()
        val sourceFactory = PagingDataSourceFactory(executor)

        val listing: Listing<Vo> = Listing(
            pagedList = sourceFactory.toLiveData(
                config = Config(
                    pageSize = pageSize,
                    initialLoadSizeHint = pageSize * 2,
                    prefetchDistance = 1,
                    enablePlaceholders = false
                ),
                fetchExecutor = executor
            ),
            refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) { it.refreshState },
            loadMoreState = Transformations.switchMap(sourceFactory.sourceLiveData) { it.loadMoreState },
            refresh = { sourceFactory.sourceLiveData.value?.invalidate() },
            retry = { sourceFactory.sourceLiveData.value?.retryAllFailed() }
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

    protected abstract suspend fun loadData(
        pageSize: Int,
        offset: Int,
        onSuccess: (List<Vo>, Int) -> Unit,
        onFailure: (Throwable) -> Unit
    )

    inner class PagingDataSourceFactory(private val retryExecutor: Executor) :
        DataSource.Factory<Int, Vo>() {

        val sourceLiveData = MutableLiveData<PagingDataSource>()

        override fun create(): DataSource<Int, Vo> {
            val source = PagingDataSource(retryExecutor)

            sourceLiveData.postValue(source)

            return source
        }
    }

    inner class PagingDataSource(private val retryExecutor: Executor) :
        PageKeyedDataSource<Int, Vo>() {

        // keep a function reference for the retry event
        private var retry: (() -> Any)? = null

        private var offset = 0

        val refreshState = MutableLiveData<LoadState>()
        val loadMoreState = MutableLiveData<LoadState>()

        fun retryAllFailed() {
            val prevRetry = retry

            retry = null

            prevRetry?.let {
                retryExecutor.execute {
                    it.invoke()
                }
            }
        }

        override fun loadInitial(
            params: LoadInitialParams<Int>,
            callback: LoadInitialCallback<Int, Vo>
        ) {
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

            this@PagingViewModel.viewModelScope.launch(Dispatchers.IO) {
                try {
                    loadData(params.requestedLoadSize, 0,
                        onSuccess = { items, _ ->
                            this@PagingDataSource.offset = items.size

                            callback.onResult(items, 0, this@PagingDataSource.offset)

                            retry = null

                            when {
                                items.isEmpty() -> {
                                    refreshState.postValue(LoadState.SUCCESS_EMPTY)
                                    loadMoreState.postValue(LoadState.IDLE)
                                }

                                items.size < params.requestedLoadSize -> {
                                    refreshState.postValue(LoadState.SUCCESS_NO_MORE)
                                    // 为了底部显示没有更多一行
                                    loadMoreState.postValue(LoadState.SUCCESS_NO_MORE)
                                }

                                else -> {
                                    refreshState.postValue(LoadState.SUCCESS)
                                    loadMoreState.postValue(LoadState.IDLE)
                                }
                            }
                        },
                        onFailure = { throwable ->
                            retry = { loadInitial(params, callback) }

                            refreshState.postValue(LoadState.failed(throwable))
                        })

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, Vo>) {
            val currentRefreshState = refreshState.value
            if (currentRefreshState != null
                && (currentRefreshState.status == LoadState.Status.RUNNING
                        || currentRefreshState.status == LoadState.Status.FAILED
                        || currentRefreshState.status == LoadState.Status.SUCCESS_NO_MORE)
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

            this@PagingViewModel.viewModelScope.launch(Dispatchers.IO) {
                loadData(params.requestedLoadSize, this@PagingDataSource.offset,
                    onSuccess = { items, _ ->
                        this@PagingDataSource.offset += items.size

                        callback.onResult(items, this@PagingDataSource.offset)

                        retry = null

                        loadMoreState.postValue(
                            when {
                                items.size < params.requestedLoadSize ->
                                    LoadState.SUCCESS_NO_MORE

                                else ->
                                    LoadState.SUCCESS
                            }
                        )
                    },
                    onFailure = { throwable ->
                        retry = { loadAfter(params, callback) }

                        loadMoreState.postValue(LoadState.failed(throwable))
                    })

            }
        }

        override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, Vo>) {
            // ignored, since we only ever append to our initial load
        }
    }
}