package com.violas.wallet.repository.http.interceptor

import android.os.Build
import com.violas.wallet.BuildConfig
import com.violas.wallet.R
import com.violas.wallet.getString
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import com.violas.wallet.utils.getUniquePseudoID
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Created by elephant on 2019-11-13 11:06.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 请求头拦截器
 */
class RequestHeaderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val newRequest = originalRequest.newBuilder()
            .header("user-agent", getString(R.string.http_user_agent))
            .header("app-ver-name", BuildConfig.VERSION_NAME)
            .header("app-ver-code", BuildConfig.VERSION_CODE.toString())
            .header("sys-ver-code", Build.VERSION.SDK_INT.toString())
            .header("location", MultiLanguageUtility.getInstance().localTag)
            .header("timestamp", System.currentTimeMillis().toString())
            .header("device-id", getUniquePseudoID())
            .build()

        return chain.proceed(newRequest)
    }
}