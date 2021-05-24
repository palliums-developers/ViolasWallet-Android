package com.violas.wallet.repository.http.bitcoin.blockCypher

import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Created by elephant on 4/27/21 4:50 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface BitcoinBlockCypherApi {

    @GET("v1/btc/{network}/addrs/{address}/balance")
    fun getAccountState(
        @Path("network") network: String,
        @Path("address") address: String
    ): Observable<AccountStateResponse>

    @GET("v1/btc/{network}/addrs/{address}?unspentOnly=true&includeScript=true")
    fun getUTXO(
        @Path("network") network: String,
        @Path("address") address: String
    ): Observable<UTXOsResponse>

    @GET("v1/btc/{network}/txs/{txid}?includeHex=true")
    fun getTransaction(
        @Path("network") network: String,
        @Path("txid") txId: String
    ): Observable<TransactionResponse>

    @POST("v1/btc/{network}/txs/push")
    fun pushTransaction(
        @Path("network") network: String,
        @Body tx: RequestBody
    ): Observable<PushTransactionResponse>

    @GET("v1/btc/{network}")
    fun getChainState(
        @Path("network") network: String
    ): Observable<ChainStateResponse>
}