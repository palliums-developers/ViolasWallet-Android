package com.violas.wallet.repository.http.bitcoin.trezor

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.palliums.exceptions.RequestException
import com.palliums.net.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Created by elephant on 2020/6/5 18:11.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Trezor repository
 */
class BitcoinTrezorRepository(private val api: BitcoinTrezorApi) {

    @Throws(RequestException::class)
    suspend fun getAccountState(
        address: String
    ) =
        api.getAccountState(address).await()

    @Throws(RequestException::class)
    suspend fun getUTXO(
        address: String
    ): List<UTXODTO> {
        return api.getUTXO(address).map {
            try {
                Gson().fromJson<List<UTXODTO>>(it)
            } catch (e: Exception) {
                try {
                    val response = Gson().fromJson<Response>(it)
                    throw RequestException(errorMsg = response.errorMsg)
                } catch (e: Exception) {
                    throw RequestException()
                }
            }
        }.await()
    }

    @Throws(RequestException::class)
    suspend fun getTransaction(
        txId: String
    ) =
        api.getTransaction(txId).await()

    @Throws(RequestException::class)
    suspend fun pushTransaction(
        tx: String
    ): String {
        val txId = api.pushTransaction(
            tx.toRequestBody("application/json".toMediaTypeOrNull())
        ).await().result

        if (txId.isNullOrBlank())
            throw RequestException.responseDataError("result cannot be null or empty")

        return txId
    }

    @Throws(RequestException::class)
    suspend fun getTransactions(
        address: String,
        pageSize: Int,
        pageNumber: Int
    ) =
        api.getTransactions(address, pageSize, pageNumber).await()

}