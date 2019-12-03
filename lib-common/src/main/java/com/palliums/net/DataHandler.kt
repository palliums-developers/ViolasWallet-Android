package com.palliums.net

/**
 * Created by elephant on 2019-11-07 18:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

@Throws(RequestException::class)
suspend inline fun <T, R> T.checkResponse(
    crossinline func: suspend T.() -> R
): R {
    try {
        val func1 = func()
        if (func1 !is ApiResponse) {
            throw RequestException.responseDataException()
        }

        if (func1.getErrorCode() != func1.getSuccessCode()) {
            throw RequestException(func1)
        }

        return func1
    } catch (e: Throwable) {
        throw if (e is RequestException) e else RequestException(e)
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
                Result.failure(RequestException(func1))
            }
        } else {
            Result.failure(RequestException.responseDataException())
        }
    } catch (e: Throwable) {
        Result.failure(RequestException(e))
    }
}
