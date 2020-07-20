package org.palliums.violascore.http

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Created by elephant on 2020/3/30 18:29.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface ViolasRpcApi {

    @Headers(value = ["urlname: violas", "Content-Type: application/json"])
    @POST("/")
    suspend fun getAccountState(@Body body: RequestDTO): Response<AccountStateDTO>

    @Headers(value = ["urlname: violas", "Content-Type: application/json"])
    @POST("/")
    suspend fun getCurrencies(@Body body: RequestDTO): Response<List<CurrenciesDTO>>

    @Headers(value = ["urlname: violas", "Content-Type: application/json"])
    @POST("/")
    suspend fun getTransaction(@Body body: RequestDTO): Response<GetTransactionDTO>

    @Headers(value = ["urlname: violas", "Content-Type: application/json"])
    @POST("/")
    suspend fun submitTransaction(@Body body: RequestDTO): Response<Any>
}