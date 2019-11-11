package com.smallraw.core.http.libra

import com.smallraw.core.http.BaseRequest
import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LibraApi {
    @GET("1.0/libra/balance?addr={address}")
    fun getBalance(@Path("address") address: String): Observable<BaseRequest<BalanceResponse>>

    @GET("1.0/libra/seqnum?addr={address}")
    fun getSequenceNumber(@Path("address") address: String): Observable<BaseRequest<Long>>

    @POST("1.0/libra/transaction")
    fun pushTx(@Body address: RequestBody): Observable<BaseRequest<Any>>

    @POST("1.0/libra/transaction?addr={address}&limit={limit}&offset={offset}")
    fun loadTransaction(
        @Path("address") address: String,
        @Path("limit") limit: Int,
        @Path("offset") offset: Int
    ):
            Observable<BaseRequest<List<TransactionResponse>>>
}