package com.violas.wallet.repository.http.dex

import com.palliums.net.RequestException
import com.palliums.net.checkResponse

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
        pageSize: String,
        lastVersion: String? = null,
        orderState: String? = null,
        giveTokenAddress: String? = null,
        getTokenAddress: String? = null
    ): ListResponse<DexOrderDTO> {

        return checkResponse {
            dexApi.getMyOrders(
                accountAddress,
                pageSize,
                lastVersion,
                orderState,
                giveTokenAddress,
                getTokenAddress
            )
        }
    }

    @Throws(RequestException::class)
    suspend fun getTokenPrices(): List<DexTokenPriceDTO> {
        return try {
            dexApi.getTokenPrices()
        } catch (e: Exception) {
            throw RequestException(e)
        }
    }
}