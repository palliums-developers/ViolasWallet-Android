package com.smallraw.core.http.libra

import com.smallraw.core.http.BaseRequest
import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LibraApi {
    @GET("1.0/libra/balance")
    fun getBalance(@Query("addr") address: String): Observable<BaseRequest<BalanceResponse>>

    @GET("1.0/libra/seqnum")
    fun getSequenceNumber(@Query("addr") address: String): Observable<BaseRequest<Long>>

    @POST("1.0/libra/transaction")
    fun pushTx(@Body address: RequestBody): Observable<BaseRequest<Any>>

    @POST("1.0/libra/transaction")
    fun loadTransaction(
        @Query("addr") address: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ):
            Observable<BaseRequest<List<TransactionResponse>>>
}