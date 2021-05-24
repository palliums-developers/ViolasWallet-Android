package com.violas.wallet.repository.http.bitcoin.bitmain

import com.palliums.exceptions.RequestException
import com.palliums.net.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Created by elephant on 2019-11-07 18:57.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 比特大陆 repository
 */
class BitcoinBitmainRepository(private val api: BitcoinBitmainApi) {

    @Throws(RequestException::class)
    suspend fun getAccountState(
        address: String
    ) =
        api.getAccountState(address).await(dataNullableOnSuccess = false).data!!

    @Throws(RequestException::class)
    suspend fun getUTXO(
        address: String
    ): List<UTXODTO> {
        val pageSize = 50
        var pageNumber = 1
        val data = mutableListOf<UTXODTO>()

        do {
            val list = api.getUTXO(address, pageSize, pageNumber++).await().data?.list
            if (list != null)
                data.addAll(list)
        } while (list?.size ?: 0 >= pageSize)

        return data
    }

    @Throws(RequestException::class)
    suspend fun getTransaction(
        txId: String
    ) =
        api.getTransaction(txId).await().data
            ?: throw RequestException.requestParamError("Transaction $txId not found")

    @Throws(RequestException::class)
    suspend fun pushTransaction(
        tx: String
    ): String {
        val txId = api.pushTransaction(
            JSONObject().put("rawhex", tx).toString()
                .toRequestBody("application/json".toMediaTypeOrNull())
        ).await().data

        if (txId.isNullOrBlank())
            throw RequestException.responseDataError("data cannot be null or empty")

        return txId
    }

    @Throws(RequestException::class)
    suspend fun getTransactions(
        address: String,
        pageSize: Int,
        pageNumber: Int
    ) =
        // {"data":null,"err_no":1,"err_msg":"Resource Not Found"}
        api.getTransactions(address, pageSize, pageNumber).await(1)

}