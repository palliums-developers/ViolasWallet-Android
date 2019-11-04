package com.violas.wallet.repository.http

import androidx.annotation.IntDef

/**
 * Created by elephant on 2019-08-13 11:20.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: LoadState
 */

data class LoadState private constructor(
    @Status val status: Int,
    private val errorText: String? = null,
    private val throwable: Throwable? = null
) {

    @IntDef(
        Status.IDLE, Status.RUNNING, Status.SUCCESS,
        Status.SUCCESS_EMPTY, Status.SUCCESS_NO_MORE, Status.FAILED
    )
    annotation class Status {
        companion object {
            const val IDLE = 0              // 闲置
            const val RUNNING = 1           // 加载中
            const val SUCCESS = 2           // 加载成功
            const val SUCCESS_EMPTY = 3     // 加载成功且为空（只有刷新状态发出）
            const val SUCCESS_NO_MORE = 4   // 加载成功且没有更多
            const val FAILED = 5            // 加载失败
        }
    }

    companion object {
        val IDLE by lazy { LoadState(Status.IDLE) }
        val RUNNING by lazy { LoadState(Status.RUNNING) }
        val SUCCESS by lazy { LoadState(Status.SUCCESS) }
        val SUCCESS_EMPTY by lazy { LoadState(Status.SUCCESS_EMPTY) }
        val SUCCESS_NO_MORE by lazy { LoadState(Status.SUCCESS_NO_MORE) }
        fun failed(errorText: String?) = LoadState(Status.FAILED, errorText = errorText)
        fun failed(throwable: Throwable?) = LoadState(Status.FAILED, throwable = throwable)
    }

    fun isNoNetwork(): Boolean {
        return throwable != null && throwable is HttpException
                && throwable.errorCode == HttpException.ERROR_CODE_NO_NETWORK
    }

    fun getErrorMsg(): String? {
        return throwable?.message ?: errorText
    }

    fun getErrorCode(): Int {
        return if (throwable != null && throwable is HttpException) {
            throwable.errorCode
        } else {
            HttpException.ERROR_CODE_UNKNOWN_ERROR
        }
    }
}