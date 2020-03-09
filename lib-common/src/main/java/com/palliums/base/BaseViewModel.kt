package com.palliums.base

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.EnhancedMutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palliums.net.LoadState
import com.palliums.net.RequestException
import com.palliums.net.getErrorTipsMsg
import com.palliums.utils.isNetworkConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2019-11-05 10:54.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
abstract class BaseViewModel : ViewModel() {

    protected val lock: Any = Any()
    private var retry: (() -> Any)? = null

    val loadState by lazy { EnhancedMutableLiveData<LoadState>() }
    val tipsMessage by lazy { EnhancedMutableLiveData<String>() }

    /**
     * 执行
     * @param params 执行的参数
     * @param action 执行的动作
     * @param checkParamBeforeExecute  执行前检查参数[params]，为true时，且[checkParams]返回true，则中断执行，反之继续执行
     * @param checkNetworkBeforeExecute 执行前检查网络，为true时，且检查网络未连接，则中断执行并抛出错误，反之继续执行
     * @param failureCallback 失败回调
     * @param successCallback 成功回调
     * @return 返回true才会调用[realExecute]去真正执行
     */
    @MainThread
    fun execute(
        vararg params: Any,
        action: Int = -1,
        checkParamBeforeExecute: Boolean = true,
        checkNetworkBeforeExecute: Boolean = true,
        failureCallback: ((error: Throwable) -> Unit)? = null,
        successCallback: (() -> Unit)? = null
    ): Boolean {
        synchronized(lock) {
            if (loadState.value?.peekData()?.status == LoadState.Status.RUNNING) {
                return false
            } else if (checkParamBeforeExecute && !checkParams(action, *params)) {
                return false
            } else if (checkNetworkBeforeExecute && !isNetworkConnected()) {
                retry = {
                    execute(
                        *params,
                        action = action,
                        checkParamBeforeExecute = checkParamBeforeExecute
                    )
                }

                val exception = RequestException.networkUnavailable()
                loadState.postValueSupport(LoadState.failure(exception))
                tipsMessage.postValueSupport(exception.getErrorTipsMsg())

                failureCallback?.invoke(exception)
                return false
            }

            loadState.postValueSupport(LoadState.RUNNING)
        }

        viewModelScope.launch(Dispatchers.Main) {
            try {

                withContext(Dispatchers.IO) { realExecute(action, *params) }

                synchronized(lock) {
                    retry = null

                    loadState.postValueSupport(LoadState.SUCCESS)
                }

                successCallback?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()

                synchronized(lock) {
                    retry = {
                        execute(
                            *params,
                            action = action,
                            checkParamBeforeExecute = checkParamBeforeExecute
                        )
                    }

                    loadState.postValueSupport(LoadState.failure(e))
                    tipsMessage.postValueSupport(e.getErrorTipsMsg())
                }

                failureCallback?.invoke(e)
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
    open fun checkParams(action: Int, vararg params: Any): Boolean {
        return true
    }

    /**
     * 真实执行
     */
    @WorkerThread
    @Throws(Throwable::class)
    protected abstract suspend fun realExecute(action: Int, vararg params: Any)
}