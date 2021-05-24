package com.violas.wallet.repository.http.bitcoin.blockCypher

import com.palliums.exceptions.RequestException
import com.palliums.net.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Created by elephant on 4/27/21 4:51 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BitcoinBlockCypherRepository(private val api: BitcoinBlockCypherApi) {

    @Throws(RequestException::class)
    suspend fun getAccountState(
        network: String,
        address: String,
    ) =
        api.getAccountState(network, address).await()

    @Throws(RequestException::class)
    suspend fun getUTXO(
        network: String,
        address: String
    ) =
        api.getUTXO(network, address).await()

    @Throws(RequestException::class)
    suspend fun getTransaction(
        network: String,
        txId: String
    ) =
        api.getTransaction(network, txId).await()

    @Throws(RequestException::class)
    suspend fun pushTransaction(
        network: String,
        tx: String
    ): String {
       val txId = api.pushTransaction(
            network,
            JSONObject().put("tx", tx).toString()
                .toRequestBody("application/json".toMediaTypeOrNull())
        ).await().hash

        if(txId.isNullOrBlank())
            throw RequestException.responseDataError("hash cannot be null or empty")

        return txId
    }

    @Throws(RequestException::class)
    suspend fun getChainState(
        network: String
    ) =
        api.getChainState(network).await()
}