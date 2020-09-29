package org.palliums.libracore.http

import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Created by elephant on 2020/3/30 18:29.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface LibraRpcApi {

    @Headers(value = ["urlname: libra", "Content-Type: application/json"])
    @POST("/")
    fun getAccountState(@Body body: RequestDTO): Observable<Response<AccountStateDTO>>

    @Headers(value = ["urlname: libra", "Content-Type: application/json"])
    @POST("/")
    fun getCurrencies(@Body body: RequestDTO): Observable<Response<List<CurrenciesDTO>>>

    @Headers(value = ["urlname: libra", "Content-Type: application/json"])
    @POST("/")
    fun getTransaction(@Body body: RequestDTO): Observable<Response<GetTransactionDTO>>

    @Headers(value = ["urlname: libra", "Content-Type: application/json"])
    @POST("/")
    fun submitTransaction(@Body body: RequestDTO): Observable<Response<Any>>
}