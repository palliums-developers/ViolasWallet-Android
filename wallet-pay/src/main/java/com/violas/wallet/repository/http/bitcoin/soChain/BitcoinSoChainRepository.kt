package com.violas.wallet.repository.http.bitcoin.soChain

import com.palliums.exceptions.RequestException
import com.palliums.net.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Created by elephant on 4/29/21 4:48 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BitcoinSoChainRepository(private val api: BitcoinSoChainApi) {

    @Throws(RequestException::class)
    suspend fun getAccountState(
        network: String,
        address: String
    ) =
        api.getAccountState(network, address).await(dataNullableOnSuccess = false).data!!

    @Throws(RequestException::class)
    suspend fun getUTXO(
        network: String,
        address: String
    ) =
        api.getUTXO(network, address).await().data?.utxos ?: listOf()

    @Throws(RequestException::class)
    suspend fun getTransaction(
        network: String,
        txId: String
    ) =
        api.getTransaction(network, txId).await().data
            ?: throw RequestException.requestParamError("Transaction $txId not found")

    @Throws(RequestException::class)
    suspend fun pushTransaction(
        network: String,
        tx: String
    ): String {
        val txId = api.pushTransaction(
            network,
            JSONObject().put("tx_hex", tx).toString()
                .toRequestBody("application/json".toMediaTypeOrNull())
        ).await().data?.txid

        if (txId.isNullOrBlank())
            throw RequestException.responseDataError("data.txid cannot be null or empty")

        return txId
    }
}