package com.palliums.listing

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palliums.net.LoadState
import com.palliums.net.NetworkException
import com.palliums.utils.isNetworkConnected
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
    val listData = MutableLiveData<MutableList<VO>>()

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

                val exception = NetworkException.networkUnavailable()
                loadState.value = LoadState.failure(exception)
                tipsMessage.value = exception.message
                return false
            }

            loadState.postValue(LoadState.RUNNING)
        }

        viewModelScope.launch {
            try {
                loadData(*params,
                    onSuccess = {
                        synchronized(lock) {
                            loadState.postValue(LoadState.SUCCESS)
                            listData.postValue(it)
                        }
                    },
                    onFailure = {
                        synchronized(lock) {
                            retry = { execute(*params) }

                            loadState.postValue(LoadState.failure(it))
                            tipsMessage.postValue(it.message)
                        }
                    })

            } catch (e: Exception) {
                e.printStackTrace()

                synchronized(lock) {
                    retry = { execute(*params) }

                    loadState.postValue(LoadState.failure(e))
                    tipsMessage.postValue(e.message)
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
    protected abstract suspend fun loadData(
        vararg params: Any,
        onSuccess: (MutableList<VO>) -> Unit,
        onFailure: (Throwable) -> Unit
    )
}