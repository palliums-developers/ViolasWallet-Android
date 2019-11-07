package com.violas.wallet.ui.record

import com.quincysx.crypto.CoinTypes

/**
 * Created by elephant on 2019-11-07 11:44.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易记录的ViewObject
 */
data class TransactionRecordVO(
    val id: Int,
    val coinTypes: CoinTypes,
    val transactionType: Int,
    val time: Long,
    val amount: Long,
    val address: String
)