package com.violas.wallet.base.paging

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

        val realPageSize = if (pageSize <= 0) PAGE_SIZE else pageSize
        val executor = ArchTaskExecutor.getIOThreadExecutor()
        val sourceFactory = PagingDataSourceFactory(executor)

        val listing: PagingData<VO> = PagingData(
            pagedList = sourceFactory.toLiveData(
                config = Config(
                    pageSize = realPageSize,
                    initialLoadSizeHint =
                    if (realPageSize >= PAGE_SIZE * 2) realPageSize else realPageSize * 2,
                    prefetchDistance =
                    if (realPageSize < PAGE_SIZE) 0 else realPageSize / 5,
                    enablePlaceholders = false
                ),
                fetchExecutor = executor
            ),
            refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) { it.refreshState },
            loadMoreState = Transformations.switchMap(sourceFactory.sourceLiveData) { it.loadMoreState },
            tipsMessage = Transformations.switchMap(sourceFactory.sourceLiveData) { it.tipsMessage },
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

    /**
     * 加载数据
     * @param pageSize 分页大小，默认为10
     * @param pageIndex 页码，默认从0开始，按接口定义自行调整
     * @param onSuccess 成功回调
     * @param onFailure 失败回调
     */
    protected abstract suspend fun loadData(
        pageSize: Int,
        pageIndex: Int,
        onSuccess: (List<VO>, Int) -> Unit,
        onFailure: (Throwable) -> Unit
    )

    inner class PagingDataSourceFactory(private val retryExecutor: Executor) :
        DataSource.Factory<Int, VO>() {

        val sourceLiveData = MutableLiveData<PagingDataSource>()

        override fun create(): DataSource<Int, VO> {
            val source = PagingDataSource(retryExecutor)

            sourceLiveData.postValue(source)

            return source
        }
    }

    inner class PagingDataSource(private val retryExecutor: Executor) :
        PageKeyedDataSource<Int, VO>() {

        // keep a function reference for the retry event
        private var retry: (() -> Any)? = null

        private var pageIndex = 0

        val refreshState = MutableLiveData<LoadState>()
        val loadMoreState = MutableLiveData<LoadState>()
        val tipsMessage = MutableLiveData<String>()

        fun retryAllFailed() {
            val prevRetry = retry

            retry = null

            prevRetry?.let {
                retryExecutor.execute { it.invoke() }
            }
        }

        override fun loadInitial(
            params: LoadInitialParams<Int>,
            callback: LoadInitialCallback<Int, VO>
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
                        onSuccess = { listData, _ ->
                            this@PagingDataSource.pageIndex = listData.size

                            callback.onResult(listData, 0, this@PagingDataSource.pageIndex)

                            retry = null

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
                        },
                        onFailure = {
                            retry = { loadInitial(params, callback) }

                            refreshState.postValue(LoadState.failure(it))
                            tipsMessage.postValue(it.message)
                        })

                } catch (e: Exception) {
                    e.printStackTrace()

                    retry = { loadInitial(params, callback) }

                    refreshState.postValue(LoadState.failure(e))
                    tipsMessage.postValue(e.message)
                }
            }
        }

        override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, VO>) {
            val currentRefreshState = refreshState.value
            if (currentRefreshState != null
                && (currentRefreshState.status == LoadState.Status.RUNNING
                        || currentRefreshState.status == LoadState.Status.FAILURE
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
                try {
                    loadData(params.requestedLoadSize, this@PagingDataSource.pageIndex,
                        onSuccess = { listData, _ ->
                            this@PagingDataSource.pageIndex += listData.size

                            callback.onResult(listData, this@PagingDataSource.pageIndex)

                            retry = null

                            loadMoreState.postValue(
                                when {
                                    listData.size < params.requestedLoadSize ->
                                        LoadState.SUCCESS_NO_MORE

                                    else ->
                                        LoadState.SUCCESS
                                }
                            )
                        },
                        onFailure = {
                            retry = { loadAfter(params, callback) }

                            loadMoreState.postValue(LoadState.failure(it))
                            tipsMessage.postValue(it.message)
                        })

                } catch (e: Exception) {
                    e.printStackTrace()

                    retry = { loadAfter(params, callback) }

                    loadMoreState.postValue(LoadState.failure(e))
                    tipsMessage.postValue(e.message)
                }
            }
        }

        override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, VO>) {
            // ignored, since we only ever append to our initial load
        }
    }
}