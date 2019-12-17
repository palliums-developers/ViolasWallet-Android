package com.palliums.listing

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palliums.net.LoadState
import com.palliums.net.RequestException
import com.palliums.utils.isNetworkConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2019-11-05 11:39.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
abstract class ListingViewModel<VO> : ViewModel() {

    private val lock: Any = Any()
    private var retry: (() -> Any)? = null

    val loadState = MutableLiveData<LoadState>()
    val tipsMessage = MutableLiveData<String>()
    val listData = MutableLiveData<List<VO>>()

    /**
     * 执行
     * @return 返回true才会调用[loadData]去加载数据
     */
    @MainThread
    fun execute(vararg params: Any): Boolean {
        synchronized(lock) {
            if (loadState.value?.status == LoadState.Status.RUNNING) {
                return false
            } else if (!checkParams(*params)) {
                return false
            } else if (checkNetworkBeforeExecute() && !isNetworkConnected()) {
                retry = { execute(*params) }

                val exception = RequestException.networkUnavailable()
                loadState.value = LoadState.failure(exception)
                tipsMessage.value = exception.message
                return false
            }

            loadState.postValue(LoadState.RUNNING)
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = loadData(*params)
                synchronized(lock) {
                    loadState.postValue(
                        if (list.isEmpty())
                            LoadState.SUCCESS_EMPTY
                        else
                            LoadState.SUCCESS
                    )
                    listData.postValue(list)
                }

            } catch (e: Exception) {
                e.printStackTrace()

                val exception = if (e is RequestException) e else RequestException(e)
                synchronized(lock) {
                    retry = { execute(*params) }

                    loadState.postValue(LoadState.failure(exception))
                    tipsMessage.postValue(exception.message)
                }
            }
        }
        return true
    }

    @MainThread
    fun retry() {
        synchronized(lock) {
            val prevRetry = retry

            retry = null

            prevRetry?.invoke()
        }
    }

    /**
     * 检查参数
     * @return 返回true继续执行，反之则中断执行
     */
    @MainThread
    protected open fun checkParams(vararg params: Any): Boolean {
        return true
    }

    /**
     * 执行前检查网络
     * @return 返回true，且检查网络未连接，则中断执行，反之继续执行
     */
    @MainThread
    protected open fun checkNetworkBeforeExecute(): Boolean {
        return true
    }

    /**
     * 加载数据
     */
    @WorkerThread
    protected abstract suspend fun loadData(vararg params: Any): List<VO>
}