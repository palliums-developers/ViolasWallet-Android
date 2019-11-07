package com.violas.wallet.repository.http

/**
 * Created by elephant on 2019-11-07 18:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

suspend inline fun <T, R> T.checkResponse(
    successCode: Int = 200,
    crossinline func: suspend T.() -> R
): Result<R> {
    return try {
        val func1 = func()
        if (func1 is BaseResponse<*>) {
            if (func1.code == successCode) {
                Result.success<R>(func1)
            } else {
                Result.failure(HttpException(func1))
            }
        } else {
            Result.failure(HttpException(ResponseDataException()))
        }
    } catch (e: Throwable) {
        Result.failure(HttpException(e))
    }
}
