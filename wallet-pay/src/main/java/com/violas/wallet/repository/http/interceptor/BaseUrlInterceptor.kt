package com.violas.wallet.repository.http.interceptor

import com.violas.wallet.common.BaseBizUrl.getDefaultBaseUrl
import com.violas.wallet.common.BaseBizUrl.getDexBaseUrl
import com.violas.wallet.common.BaseBrowserUrl.getBitmainBaseUrl
import com.violas.wallet.common.BaseBrowserUrl.getLibexplorerBaseUrl
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Created by elephant on 2019-11-07 17:35.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: BaseUrl拦截器，处理特殊请求
 */
class BaseUrlInterceptor : Interceptor {

    companion object {
        const val HEADER_KEY_URLNAME = "urlname"
        const val HEADER_VALUE_BITMAIN = "bitmain"
        const val HEADER_VALUE_LIBEXPLORER = "libexplorer"
        const val HEADER_VALUE_DEX = "dex"
    }

    private val baseUrls: Map<String, String> by lazy {
        mutableMapOf<String, String>().apply {
            this[HEADER_VALUE_DEX] = getDexBaseUrl()
            this[HEADER_VALUE_BITMAIN] = getBitmainBaseUrl()
            this[HEADER_VALUE_LIBEXPLORER] = getLibexplorerBaseUrl()
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val urlNameList = originalRequest.headers(HEADER_KEY_URLNAME)
        if (urlNameList.isEmpty()) {
            return chain.proceed(originalRequest)
        }

        val replaceUrl: HttpUrl = baseUrls[urlNameList[0]]?.toHttpUrlOrNull()
            ?: return chain.proceed(originalRequest)

        val newUrlBuilder = originalRequest.url.newBuilder()
        val originalPathSegments = ArrayList<String>(originalRequest.url.pathSegments)

        originalPathSegments.forEach { _ ->
            newUrlBuilder.removePathSegment(0)
        }

        val baseUrl = getDefaultBaseUrl().toHttpUrl()
        originalRequest.url.pathSegments.forEach { originalPathSegment ->
            baseUrl.pathSegments.forEach { basePathSegment ->
                if (originalPathSegment == basePathSegment) {
                    originalPathSegments.remove(originalPathSegment)
                }
            }
        }

        val newUrl = newUrlBuilder
            .scheme(replaceUrl.scheme)
            .host(replaceUrl.host)
            .port(replaceUrl.port)
            .apply {

                replaceUrl.pathSegments.forEach {
                    if (it.isNotEmpty()) {
                        addPathSegment(it)
                    }
                }

                originalPathSegments.forEach {
                    addPathSegment(it)
                }
            }
            .build()

        val newRequest = originalRequest.newBuilder()
            .removeHeader(HEADER_KEY_URLNAME)
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}