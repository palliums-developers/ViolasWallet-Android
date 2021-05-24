package com.violas.wallet.repository.http.bitcoin.soChain

import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Created by elephant on 4/29/21 4:47 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface BitcoinSoChainApi {

    @GET("v2/get_address_balance/{network}/{address}")
    fun getAccountState(
        @Path("network") network: String,
        @Path("address") address: String
    ): Observable<Response<AccountStateDTO>>

    @GET("v2/get_tx_unspent/{network}/{address}")
    fun getUTXO(
        @Path("network") network: String,
        @Path("address") address: String
    ): Observable<Response<UTXOsDTO>>

    @GET("v2/get_tx/{network}/{txid}")
    fun getTransaction(
        @Path("network") network: String,
        @Path("txid") txId: String
    ): Observable<Response<TransactionDTO>>

    @POST("v2/send_tx/{network}")
    fun pushTransaction(
        @Path("network") network: String,
        @Body tx: RequestBody
    ): Observable<Response<PushTransactionDTO>>
}