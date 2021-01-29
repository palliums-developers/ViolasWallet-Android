package com.palliums.base

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.EnhancedMutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palliums.exceptions.RequestException
import com.palliums.extensions.*
import com.palliums.net.LoadState
import com.palliums.utils.isNetworkConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2019-11-05 10:54.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
abstract class BaseViewModel : ViewModel() {

    companion object {
        private const val TAG = "ViewModel"
    }

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
    open fun execute(
        vararg params: Any,
        action: Int = -1,
        checkParamBeforeExecute: Boolean = true,
        checkNetworkBeforeExecute: Boolean = false,
        failureCallback: ((error: Throwable) -> Unit)? = null,
        successCallback: (() -> Unit)? = null
    ): Boolean {
        synchronized(lock) {
            if (loadState.value?.peekData()?.status == LoadState.Status.RUNNING) {
                logWarn(TAG) {
                    "execute(action = $action, param = ${
                        params.contentToString()
                    }) => executing"
                }
                return false
            } else if (checkParamBeforeExecute && !checkParams(action, *params)) {
                logError(TAG) {
                    "execute(action = $action, param = ${
                        params.contentToString()
                    }) => params abnormal"
                }
                return false
            }

            logInfo(TAG) {
                "execute(action = $action, param = ${
                    params.contentToString()
                }) => start"
            }
            loadState.setValueSupport(LoadState.RUNNING.apply { this.action = action })
        }

        val startTime = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (checkNetworkBeforeExecute && !isNetworkConnected()) {
                    throw RequestException.networkUnavailable()
                }

                withContext(Dispatchers.IO) { realExecute(action, *params) }
                logInfo(TAG) {
                    "execute(action = $action, param = ${
                        params.contentToString()
                    }) => success(${
                        System.currentTimeMillis() - startTime
                    }ms)"
                }

                synchronized(lock) {
                    retry = null
                    loadState.setValueSupport(
                        LoadState.SUCCESS.apply { this.action = action }
                    )
                }
                successCallback?.invoke()
            } catch (e: Exception) {
                val activeCancellation = e.isActiveCancellation()
                if (!activeCancellation) {
                    logError(e, TAG) {
                        "execute(action = $action, param = ${
                            params.contentToString()
                        }) => failure(${
                            System.currentTimeMillis() - startTime
                        }ms)"
                    }

                    delayOnError(startTime)
                }

                synchronized(lock) {
                    if (activeCancellation) {
                        loadState.setValueSupport(
                            LoadState.IDLE.apply { this.action = action }
                        )
                    } else {
                        retry = {
                            execute(
                                params = *params,
                                action = action,
                                checkParamBeforeExecute = checkParamBeforeExecute,
                                checkNetworkBeforeExecute = checkNetworkBeforeExecute,
                                failureCallback = failureCallback,
                                successCallback = successCallback
                            )
                        }

                        loadState.setValueSupport(
                            LoadState.failure(e).apply { this.action = action }
                        )
                        tipsMessage.setValueSupport(
                            e.getShowErrorMessage(isLoadAction(action))
                        )
                    }
                }

                if (!activeCancellation) {
                    failureCallback?.invoke(e)
                }
            }
        }
        return true
    }

    private suspend fun delayOnError(startTime: Long) {
        val spentTime = System.currentTimeMillis() - startTime
        if (spentTime < 1000) {
            try {
                delay(1000 - spentTime)
            } catch (ignore: Exception) {
            }
        }
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
     * 是否为加载数据动作，默认为其它操作动作
     * 用于[execute]执行异常时获取错误信息展示，是加载失败还是操作失败
     */
    open fun isLoadAction(action: Int): Boolean {
        return false
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