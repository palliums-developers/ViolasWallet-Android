package com.palliums.net

import com.palliums.BuildConfig
import com.palliums.R
import com.palliums.utils.getString
import java.util.*

/**
 * Created by elephant on 2019-11-07 18:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun Throwable.getErrorTipsMsg(): String {
    return when {
        this is RequestException -> {
            this.errorMsg
        }
        BuildConfig.DEBUG -> {
            String.format(
                Locale.ENGLISH,
                "%s\n%s",
                getString(R.string.common_load_fail),
                this.toString()
            )
        }
        else -> {
            getString(R.string.common_load_fail)
        }
    }
}

@Throws(RequestException::class)
suspend inline fun <T, R> T.checkResponse(
    vararg specialStatusCodes: Any,
    dataNullableOnSuccess: Boolean = true,
    crossinline func: suspend T.() -> R
): R {
    try {
        val func1 = func()
        if (func1 !is ApiResponse) {
            throw RequestException.responseDataException("Response is not ApiResponse")
        }

        if (specialStatusCodes.isNotEmpty()) {
            specialStatusCodes.forEach {
                if (func1.getErrorCode() == it) {
                    return func1
                }
            }
        }

        if (func1.getErrorCode() != func1.getSuccessCode()) {
            throw RequestException(func1)
        } else if (!dataNullableOnSuccess && func1.getResponseData() == null) {
            throw RequestException.responseDataException("Data is null")
        }

        return func1
    } catch (e: Throwable) {
        throw if (e is RequestException) e else RequestException(e)
    }
}

@Throws(RequestException::class)
suspend inline fun <T, R> T.checkResponse2(
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