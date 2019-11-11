package com.violas.wallet.repository.http

import com.violas.wallet.BuildConfig
import com.violas.wallet.repository.http.bitcoin.BitcoinApi
import com.violas.wallet.repository.http.bitcoin.BitcoinRepository
import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Created by elephant on 2019-11-07 18:41.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
object HttpInjector {

    private val okHttp by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().also {
                it.level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            .addInterceptor(BaseUrlInterceptor())
            .callTimeout(40, TimeUnit.SECONDS)
            .connectTimeout(40, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .build()
    }


    private val retrofit by lazy {
        Retrofit.Builder()
            .client(okHttp)
            .baseUrl("")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val bitCoinApi by lazy { retrofit.create(BitcoinApi::class.java) }

    val bitcoinRepository by lazy { BitcoinRepository(bitCoinApi) }

}