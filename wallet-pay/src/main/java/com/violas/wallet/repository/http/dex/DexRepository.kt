package com.violas.wallet.repository.http.dex

import com.palliums.net.RequestException
import com.palliums.net.checkResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Created by elephant on 2019-12-06 10:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易所 repository
 */
class DexRepository(private val dexApi: DexApi) {

    @Throws(RequestException::class)
    suspend fun getMyOrders(
        accountAddress: String,
        pageSize: Int,
        lastVersion: String? = null,
        orderState: String? = null,
        tokenGiveAddress: String? = null,
        tokenGetAddress: String? = null
    ): ListResponse<DexOrderDTO> {

        return checkResponse {
            dexApi.getMyOrders(
                accountAddress,
                pageSize,
                lastVersion,
                orderState,
                tokenGiveAddress,
                tokenGetAddress
            )
        }
    }

    @Throws(RequestException::class)
    suspend fun getTokens(): List<DexTokenDTO> {

        return try {
            dexApi.getTokens()
        } catch (e: Exception) {
            throw RequestException(e)
        }
    }

    @Throws(RequestException::class)
    suspend fun getOrderTrades(
        version: String,
        pageSize: Int,
        pageNumber: Int
    ): ListResponse<DexOrderTradeDTO> {

        return checkResponse {
            dexApi.getOrderTrades(version, pageSize, pageNumber)
        }
    }

    @Throws(RequestException::class)
    suspend fun revokeOrder(version: String, signedtxn: String): Response<String> {
        val requestBody =
            """{"version":"$version","signedtxn":"$signedtxn"}"""
                .toRequestBody("application/json".toMediaTypeOrNull())

        return checkResponse {
            dexApi.revokeOrder(requestBody)
        }
    }
}