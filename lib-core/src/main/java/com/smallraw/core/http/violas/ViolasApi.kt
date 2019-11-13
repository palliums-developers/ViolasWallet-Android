package com.smallraw.core.http.violas

import com.smallraw.core.http.BaseRequest
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ViolasApi {
    @GET("1.0/violas/balance")
    fun getBalance(@Query("addr") address: String): Single<BaseRequest<BalanceResponse>>

    @GET("1.0/violas/seqnum")
    fun getSequenceNumber(@Query("addr") address: String): Single<BaseRequest<Long>>

    @POST("1.0/violas/transaction")
    fun pushTx(@Body address: RequestBody): Single<BaseRequest<Any>>

    @GET("1.0/violas/transaction")
    fun loadTransaction(
        @Query("addr") address: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ):
            Single<BaseRequest<List<TransactionResponse>>>

    @GET("1.0/violas/currency")
    fun getSupportCurrency(): Single<BaseRequest<List<SupportCurrencyResponse>>>
}