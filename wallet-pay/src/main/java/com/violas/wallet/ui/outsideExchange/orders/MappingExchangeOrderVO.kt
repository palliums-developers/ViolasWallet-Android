package com.violas.wallet.ui.outsideExchange.orders

import com.violas.wallet.repository.http.mappingExchange.MappingType

/**
 * Created by elephant on 2020-02-18 12:20.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
data class MappingExchangeOrderVO(
    val id: Long,
    val type: MappingType,
    val time: Long,
    val status: Int,
    val amount: String,
    val address: String
)