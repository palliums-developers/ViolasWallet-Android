package com.palliums.net

import androidx.annotation.IntDef
import com.palliums.extensions.isNoNetwork

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
        Status.SUCCESS_EMPTY, Status.SUCCESS_NO_MORE, Status.FAILURE
    )
    annotation class Status {
        companion object {
            const val IDLE = 0              // 闲置
            const val RUNNING = 1           // 加载中
            const val SUCCESS = 2           // 加载成功
            const val SUCCESS_EMPTY = 3     // 加载成功且为空（只有刷新状态发出）
            const val SUCCESS_NO_MORE = 4   // 加载成功且没有更多
            const val FAILURE = 5           // 加载失败
        }
    }

    companion object {
        val IDLE by lazy { LoadState(Status.IDLE) }
        val RUNNING by lazy { LoadState(Status.RUNNING) }
        val SUCCESS by lazy { LoadState(Status.SUCCESS) }
        val SUCCESS_EMPTY by lazy { LoadState(Status.SUCCESS_EMPTY) }
        val SUCCESS_NO_MORE by lazy { LoadState(Status.SUCCESS_NO_MORE) }
        fun failure(errorText: String?) = LoadState(Status.FAILURE, errorText = errorText)
        fun failure(throwable: Throwable?) = LoadState(Status.FAILURE, throwable = throwable)
    }

    fun isNoNetwork(): Boolean {
        return throwable != null && throwable.isNoNetwork()
    }

    fun getErrorMsg(): String? {
        return throwable?.message ?: errorText
    }
}