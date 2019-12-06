package com.violas.wallet.repository.http.dex

import com.palliums.net.checkResponse

/**
 * Created by elephant on 2019-12-06 10:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易所 repository
 */
class DexRepository(private val dexApi: DexApi) {

    suspend fun getMyOrders(
        accountAddress: String,
        pageSize: String,
        lastVersion: String,
        orderState: String? = null,
        baseTokenAddress: String? = null,
        quoteTokenAddress: String? = null
    ): ListResponse<DexOrderDTO> {

        return checkResponse {
            dexApi.getMyOrders(
                accountAddress,
                pageSize,
                lastVersion,
                orderState,
                baseTokenAddress,
                quoteTokenAddress
            )
        }
    }

    suspend fun getTokenPrices(): ListResponse<DexTokenPriceDTO> {
        return checkResponse {
            dexApi.getTokenPrices()
        }
    }
}