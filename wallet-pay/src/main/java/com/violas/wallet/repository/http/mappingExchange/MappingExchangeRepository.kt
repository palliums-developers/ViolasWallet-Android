package com.violas.wallet.repository.http.mappingExchange

import com.palliums.net.checkResponse

/**
 * Created by elephant on 2020-02-14 11:48.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MappingExchangeRepository(private val api: MappingExchangeApi) {

    suspend fun getMappingInfo(type: MappingType) =
        checkResponse(dataNullableOnSuccess = false) {
            api.getMappingInfo(type.typeName)
        }

    suspend fun getMappingExchangeOrderNumber(type: MappingType, address: String) =
        checkResponse(dataNullableOnSuccess = false) {
            api.getMappingExchangeOrderNumber(type.typeName, address)
        }

    suspend fun getMappingExchangeOrders(
        walletAddress: String, walletType: Int, pageSize: Int, offset: Int
    ) =
        checkResponse {
            api.getMappingExchangeOrders(walletAddress, walletType, pageSize, offset)
        }
}