package com.violas.wallet.repository.http

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.BuildConfig
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.http.bitcoin.BitmainApi
import com.violas.wallet.repository.http.bitcoin.BitmainRepository
import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor
import com.violas.wallet.repository.http.libra.LibexplorerApi
import com.violas.wallet.repository.http.libra.LibexplorerRepository
import com.violas.wallet.repository.http.violas.ViolasApi
import com.violas.wallet.repository.http.violas.ViolasRepository
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

    const val BASE_URL_MAIN_NET = "http://52.27.228.84:4000/1.0/"
    const val BASE_URL_TEST_NET = "http://52.27.228.84:4000/1.0/"

    private val okHttp by lazy {
        OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor())
            .addInterceptor(HttpLoggingInterceptor().also {
                it.level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.HEADERS
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            .callTimeout(40, TimeUnit.SECONDS)
            .connectTimeout(40, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .client(okHttp)
            .baseUrl(getBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getBaseUrl(): String {
        return if (Vm.TestNet) BASE_URL_TEST_NET else BASE_URL_MAIN_NET
    }

    private val bitmainApi by lazy { retrofit.create(BitmainApi::class.java) }

    private val libexplorerApi by lazy { retrofit.create(LibexplorerApi::class.java) }

    private val violasApi by lazy { retrofit.create(ViolasApi::class.java) }

    val bitmainRepository by lazy { BitmainRepository(bitmainApi) }

    val libexplorerRepository by lazy { LibexplorerRepository(libexplorerApi) }

    val violasRepository by lazy { ViolasRepository(violasApi) }

    fun getTransactionRepository(coinTypes: CoinTypes): TransactionRepository {
        return when (coinTypes) {
            CoinTypes.VToken -> {
                violasRepository
            }

            CoinTypes.Libra -> {
                libexplorerRepository
            }

            else -> {
                bitmainRepository
            }
        }
    }
}