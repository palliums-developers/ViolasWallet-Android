package com.smallraw.core.http.violas

import com.smallraw.core.http.BaseRequest
import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ViolasApi {
    @GET("1.0/violas/balance?addr={address}")
    fun getBalance(@Path("address") address: String): Observable<BaseRequest<BalanceResponse>>

    @GET("1.0/violas/seqnum?addr={address}")
    fun getSequenceNumber(@Path("address") address: String): Observable<BaseRequest<Long>>

    @POST("1.0/violas/transaction")
    fun pushTx(@Body address: RequestBody): Observable<BaseRequest<Any>>

    @GET("1.0/violas/transaction?addr={address}&limit={limit}&offset={offset}")
    fun loadTransaction(
        @Path("address") address: String,
        @Path("limit") limit: Int,
        @Path("offset") offset: Int
    ):
            Observable<BaseRequest<List<TransactionResponse>>>

    @GET("1.0/wallet/violas/currency")
    fun getSupportCurrency(): Observable<BaseRequest<List<SupportCurrencyResponse>>>
}