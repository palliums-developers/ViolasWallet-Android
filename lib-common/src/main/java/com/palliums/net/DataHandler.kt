package com.palliums.net

import com.palliums.exceptions.RequestException
import com.palliums.extensions.logError
import com.palliums.extensions.logInfo
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

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
        throw RequestException.responseDataError("Response is not ApiResponse")
    }

    specialStatusCodes.forEach {
        if (response.getErrorCode() == it) {
            return response
        }
    }

    if (!response.isSuccess()) {
        if (checkError != null) {
            checkError.invoke(response)
        } else {
            throw RequestException(response)
        }
    } else if (!dataNullableOnSuccess && response.getResponseData() == null) {
        throw RequestException.responseDataError("Data is null")
    }

    return response
}

suspend inline fun <T> Observable<T>.await(
    vararg specialStatusCodes: Any,
    dataNullableOnSuccess: Boolean = true,
    noinline customError: ((response: T) -> Unit)? = null
): T {
    return suspendCancellableCoroutine { continuation ->

        val disposable = subscribeOn(Schedulers.io())
            .subscribe({ response ->
                logInfo("ObservableConverter") {
                    "onResponse. is cancelled => ${continuation.isCancelled}"
                }
                if (continuation.isCancelled) return@subscribe

                continuation.resumeWith(runCatching {
                    if (response !is ApiResponse) {
                        return@runCatching response
                    }

                    specialStatusCodes.forEach {
                        if (response.getErrorCode() == it) {
                            return@runCatching response
                        }
                    }

                    if (!response.isSuccess()) {
                        if (customError != null) {
                            customError.invoke(response)
                        } else {
                            throw RequestException(response)
                        }
                    } else if (!dataNullableOnSuccess && response.getResponseData() == null) {
                        throw RequestException.responseDataError("Data is null")
                    }

                    return@runCatching response
                })
            }, {
                logError("ObservableConverter") {
                    "onFailure. is cancelled => ${continuation.isCancelled}, $it"
                }
                if (continuation.isCancelled) return@subscribe

                continuation.resumeWithException(RequestException(it))
            })

        continuation.invokeOnCancellation {
            logInfo("ObservableConverter") {
                "invokeOnCancellation. cancel the request"
            }
            try {
                disposable.dispose()
            } catch (ignore: Exception) {
            }
        }
    }
}