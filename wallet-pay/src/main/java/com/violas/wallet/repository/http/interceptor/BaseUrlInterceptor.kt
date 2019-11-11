package com.violas.wallet.repository.http.interceptor

import com.violas.wallet.common.Vm
import okhttp3.HttpUrl
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
        const val HEADER_KEY_URL_NAME = "urlName"
        const val HEADER_VALUE_BTC = "BTC"

        const val BTC_URL_MAIN_NET = "https://chain.api.btc.com/v3/"
        const val BTC_URL_TEST_NET = "https://testnet-chain.api.btc.com/v3/"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val urlNameList = originalRequest.headers(HEADER_KEY_URL_NAME)
        if (urlNameList.isEmpty()) {
            return chain.proceed(originalRequest)
        }

        var baseUrl: HttpUrl? = null
        if (urlNameList[0] == HEADER_VALUE_BTC) {
            val url = if (Vm.TestNet) BTC_URL_TEST_NET else BTC_URL_MAIN_NET
            baseUrl = url.toHttpUrlOrNull()
        }

        if (baseUrl == null) {
            return chain.proceed(originalRequest)
        }

        val newUrl = originalRequest.url.newBuilder()
            .scheme(baseUrl.scheme)
            .host(baseUrl.host)
            .port(baseUrl.port)
            .build()

        val newRequest = originalRequest.newBuilder()
            .removeHeader(HEADER_KEY_URL_NAME)
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}