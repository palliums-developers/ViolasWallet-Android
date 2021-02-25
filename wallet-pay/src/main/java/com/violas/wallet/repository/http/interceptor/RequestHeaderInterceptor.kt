package com.violas.wallet.repository.http.interceptor

import com.palliums.utils.getHttpUserAgent
import com.palliums.utils.getUniquePseudoID
import com.violas.wallet.BuildConfig
import com.violas.wallet.common.getDiemChainId
import com.violas.wallet.common.getViolasChainId
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Created by elephant on 2019-11-13 11:06.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 请求头拦截器
 */
class RequestHeaderInterceptor(private val closeConnection: Boolean = true) : Interceptor {

    companion object {
        const val HEADER_KEY_CHAIN_NAME = "chainName"
        const val HEADER_VALUE_VIOLAS_CHAIN = "violas"
        const val HEADER_VALUE_DIEM_CHAIN = "diem"

        private const val HEADER_KEY_CHAIN_ID = "chainId"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val chainId = originalRequest.header(HEADER_KEY_CHAIN_NAME)
        val newRequest = originalRequest.newBuilder()
            .removeHeader(HEADER_KEY_CHAIN_NAME)
            .header("User-Agent", getHttpUserAgent())
            .header("platform", "android")
            .header("bundleId", BuildConfig.APPLICATION_ID)
            .header("versionName", BuildConfig.VERSION_NAME)
            .header("versionCode", BuildConfig.VERSION_CODE.toString())
            .header("language", MultiLanguageUtility.getInstance().localTag)
            .header("timestamp", System.currentTimeMillis().toString())
            .header("deviceId", getUniquePseudoID()).apply {
                if (closeConnection) {
                    header("Connection", "close")
                }
                when (chainId) {
                    HEADER_VALUE_VIOLAS_CHAIN -> {
                        header(HEADER_KEY_CHAIN_ID, getViolasChainId().toString())
                    }
                    HEADER_VALUE_DIEM_CHAIN -> {
                        header(HEADER_KEY_CHAIN_ID, getDiemChainId().toString())
                    }
                }
            }
            .build()

        return chain.proceed(newRequest)
    }
}