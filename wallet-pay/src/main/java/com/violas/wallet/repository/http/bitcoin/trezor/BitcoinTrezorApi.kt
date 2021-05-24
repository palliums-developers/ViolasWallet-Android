package com.violas.wallet.repository.http.bitcoin.trezor

import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor.Companion.HEADER_KEY_URL_NAME
import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor.Companion.HEADER_VALUE_TREZOR
import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * Created by elephant on 2020/6/5 17:44.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Trezor api
 * @see <a href="https://github.com/trezor/blockbook/blob/master/docs/api.md">link</a>
 */
interface BitcoinTrezorApi {

    @GET("v2/address/{address}?details=basic")
    fun getAccountState(
        @Path("address") address: String
    ): Observable<AccountStateResponse>

    @GET("v2/utxo/{address}")
    fun getUTXO(
        @Path("address") address: String
    ): Observable<String>

    @GET("v2/tx/{txid}")
    fun getTransaction(
        @Path("txid") txId: String
    ): Observable<TransactionResponse>

    @POST("v2/sendtx")
    fun pushTransaction(
        @Body tx: RequestBody
    ): Observable<PushTransactionResponse>

    /**
     * 获取指定地址的交易记录，分页查询
     * @param address 地址
     * @param pageSize 分页大小
     * @param pageNumber 页码，从1开始
     */
    @Headers(value = ["${HEADER_KEY_URL_NAME}:${HEADER_VALUE_TREZOR}"])
    @GET("v2/address/{address}?details=txs")
    fun getTransactions(
        @Path("address") address: String,
        @Query("pageSize") pageSize: Int,
        @Query("page") pageNumber: Int
    ): Observable<TransactionsResponse>

}