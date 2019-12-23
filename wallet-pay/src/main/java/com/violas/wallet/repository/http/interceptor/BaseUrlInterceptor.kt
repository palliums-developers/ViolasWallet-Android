package com.violas.wallet.repository.http.interceptor

import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
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

        // 对外公开，有API速率限制（每分钟120次）
        private const val BITMAIN_BASE_URL_MAIN_NET_OPEN = "https://chain.api.btc.com/v3/"
        private const val BITMAIN_BASE_URL_TEST_NET_OPEN = "https://testnet-chain.api.btc.com/v3/"
        // 正式对接，无API速率限制
        private const val BITMAIN_BASE_URL_MAIN_NET =
            "https://developer-btc-chain.api.btc.com/appkey-e6e2ce95d8df/"
        private const val BITMAIN_BASE_URL_TEST_NET = "https://tchain.api.btc.com/v3/"

        private const val LIBEXPLORER_BASE_URL_MAIN_NET = "https://api-test.libexplorer.com/api"
        private const val LIBEXPLORER_BASE_URL_TEST_NET = "https://api-test.libexplorer.com/api"

        private const val DEX_BASE_URL_MAIN_NET = "http://18.220.66.235:38181"
        private const val DEX_BASE_URL_TEST_NET = "http://18.220.66.235:38181"

        private const val BITCOIN_BROWSER_BASE_URL_BLOCKCYPHER_MAIN_NET =
            "https://live.blockcypher.com/btc"
        private const val BITCOIN_BROWSER_BASE_URL_BLOCKCYPHER_TEST_NET =
            "https://live.blockcypher.com/btc-testnet"

        private const val LIBRA_BROWSER_BASE_URL_LIBEXPLORER_MAIN_NET =
            "https://libexplorer.com"
        private const val LIBRA_BROWSER_BASE_URL_LIBEXPLORER_TEST_NET =
            "https://libexplorer.com"

        fun getDexBaseUrl(): String {
            return if (Vm.TestNet)
                DEX_BASE_URL_TEST_NET
            else
                DEX_BASE_URL_MAIN_NET
        }

        fun getBitmainOpenBaseUrl(): String {
            return if (Vm.TestNet)
                BITMAIN_BASE_URL_TEST_NET_OPEN
            else
                BITMAIN_BASE_URL_MAIN_NET_OPEN
        }

        fun getBitmainBaseUrl(): String {
            return if (Vm.TestNet)
                BITMAIN_BASE_URL_TEST_NET
            else
                BITMAIN_BASE_URL_MAIN_NET
        }

        fun getLibexplorerBaseUrl(): String {
            return if (Vm.TestNet)
                LIBEXPLORER_BASE_URL_TEST_NET
            else
                LIBEXPLORER_BASE_URL_MAIN_NET
        }

        fun getBitcoinBrowserUrl(transactionHash: String): String {
            return "${if (Vm.TestNet)
                BITCOIN_BROWSER_BASE_URL_BLOCKCYPHER_TEST_NET
            else
                BITCOIN_BROWSER_BASE_URL_BLOCKCYPHER_MAIN_NET}/tx/$transactionHash"
        }

        fun getLibraBrowserUrl(version: String): String {
            return "${if (Vm.TestNet)
                LIBRA_BROWSER_BASE_URL_LIBEXPLORER_TEST_NET
            else
                LIBRA_BROWSER_BASE_URL_LIBEXPLORER_MAIN_NET}/version/$version"
        }
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

        val baseUrl = DataRepository.getDefaultBaseUrl().toHttpUrl()
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