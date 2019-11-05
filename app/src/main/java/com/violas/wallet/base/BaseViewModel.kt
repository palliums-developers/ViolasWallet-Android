package com.violas.wallet.base

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violas.wallet.repository.http.HttpException
import com.violas.wallet.repository.http.LoadState
import com.violas.wallet.utils.isNetworkConnected
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2019-11-05 10:54.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
abstract class BaseViewModel : ViewModel() {

    private var retry: (() -> Any)? = null

    val loadState = MutableLiveData<LoadState>()
    val tipsMessage = MutableLiveData<String>()

    /**
     * 执行
     * @return 返回true才会调用[loadData]去加载数据
     */
    @MainThread
    fun execute(vararg params: Any?): Boolean {
        if (loadState.value?.status == LoadState.Status.RUNNING) {
            return false
        } else if (!checkParams(*params)) {
            return false
        } else if (checkNetworkBeforeExecution() && !isNetworkConnected()) {
            retry = { execute(*params) }

            val exception = HttpException.networkUnavailable()
            loadState.value = LoadState.failed(exception)
            tipsMessage.value = exception.message
            return false
        }

        loadState.postValue(LoadState.RUNNING)
        viewModelScope.launch {
            loadData(*params,
                onSuccess = {
                    loadState.postValue(LoadState.SUCCESS)
                },
                onFailure = {
                    retry = { execute(*params) }

                    loadState.postValue(LoadState.failed(it))
                    tipsMessage.postValue(it.message)
                })
        }
        return true
    }

    @MainThread
    fun retry() {
        val prevRetry = retry

        retry = null

        prevRetry?.invoke()
    }

    /**
     * 检查参数
     * @return 返回true继续执行，反之则中断执行
     */
    @MainThread
    protected open fun checkParams(vararg params: Any?): Boolean {
        return true
    }

    /**
     * 执行前检查网络
     * @return 返回true，且检查网络未连接，则中断执行，反之继续执行
     */
    @MainThread
    protected open fun checkNetworkBeforeExecution(): Boolean {
        return true
    }

    /**
     * 加载数据
     */
    @WorkerThread
    protected abstract suspend fun loadData(
        vararg params: Any?,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    )
}