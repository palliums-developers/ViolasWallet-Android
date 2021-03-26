package com.violas.wallet.biz.transaction

/**
 * Created by elephant on 3/24/21 11:02 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

data class ViolasGasInfo(
    val gasCurrencyCode: String,
    val maxGasAmount: Long,
    val gasUnitPrice: Long
)