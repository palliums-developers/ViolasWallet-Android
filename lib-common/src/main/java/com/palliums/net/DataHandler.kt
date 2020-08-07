package com.palliums.net

import com.palliums.exceptions.RequestException
import com.palliums.extensions.lazyLogError
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

suspend inline fun <T> Observable<T>.await(
    vararg specialStatusCodes: Any,
    dataNullableOnSuccess: Boolean = true,
    noinline customError: ((response: T) -> Throwable)? = null
): T {
    return suspendCancellableCoroutine { continuation ->

        val disposable = subscribeOn(Schedulers.io())
            .subscribe({ response ->
                lazyLogError("ObservableConverter") {
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

                    if (response.getErrorCode() != response.getSuccessCode()) {
                        throw customError?.invoke(response) ?: RequestException(response)
                    } else if (!dataNullableOnSuccess && response.getResponseData() == null) {
                        throw RequestException.responseDataException("Data is null")
                    }

                    return@runCatching response
                })
            }, {
                lazyLogError("ObservableConverter") {
                    "onFailure. is cancelled => ${continuation.isCancelled}, $it"
                }
                if (continuation.isCancelled) return@subscribe

                continuation.resumeWithException(RequestException(it))
            })

        continuation.invokeOnCancellation {
            lazyLogError("ObservableConverter") {
                "invokeOnCancellation. cancel the request"
            }
            try {
                disposable.dispose()
            } catch (ignore: Exception) {
            }
        }
    }
}