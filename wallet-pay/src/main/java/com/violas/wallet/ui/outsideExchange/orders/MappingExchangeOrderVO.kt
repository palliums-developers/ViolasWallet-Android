package com.violas.wallet.ui.outsideExchange.orders

/**
 * Created by elephant on 2020-02-18 12:20.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
data class MappingExchangeOrderVO(
    val id: Int,
    val time: Long,
    val status: Int,
    val amount: String,
    val address: String,
    val coinName: String
)