package com.palliums.net

/**
 * Created by elephant on 2019-11-07 18:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

@Throws(NetworkException::class)
suspend inline fun <T, R> T.checkResponse(
    crossinline func: suspend T.() -> R
): R {
    try {
        val func1 = func()
        if (func1 is ApiResponse) {
            if (func1.getErrorCode() == func1.getSuccessCode()) {
                return func1
            } else {
                throw NetworkException(func1)
            }
        } else {
            throw NetworkException.responseDataException()
        }
    } catch (e: Throwable) {
        throw NetworkException(e)
    }
}

suspend inline fun <T, R> T.checkResponseWithResult(
    crossinline func: suspend T.() -> R
): Result<R> {
    return try {
        val func1 = func()
        if (func1 is ApiResponse) {
            if (func1.getErrorCode() == func1.getSuccessCode()) {
                Result.success<R>(func1)
            } else {
                Result.failure(NetworkException(func1))
            }
        } else {
            Result.failure(NetworkException.responseDataException())
        }
    } catch (e: Throwable) {
        Result.failure(NetworkException(e))
    }
}
