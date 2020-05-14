package com.palliums.net

import com.palliums.exceptions.RequestException

/**
 * Created by elephant on 2019-11-07 18:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

@Throws(RequestException::class)
suspend inline fun <T, R> T.checkResponse(
    vararg specialStatusCodes: Any,
    dataNullableOnSuccess: Boolean = true,
    noinline checkError: ((response: R) -> Unit)? = null,
    crossinline func: suspend T.() -> R
): R {
    val response = try {
        func()
    } catch (e: Throwable) {
        throw RequestException(e)
    }

    if (response !is ApiResponse) {
        throw RequestException.responseDataException("Response is not ApiResponse")
    }

    specialStatusCodes.forEach {
        if (response.getErrorCode() == it) {
            return response
        }
    }

    if (response.getErrorCode() != response.getSuccessCode()) {
        if (checkError != null) {
            checkError.invoke(response)
        } else {
            throw RequestException(response)
        }
    } else if (!dataNullableOnSuccess && response.getResponseData() == null) {
        throw RequestException.responseDataException("Data is null")
    }

    return response
}