package com.violas.wallet.ui.transactionRecord

import com.quincysx.crypto.CoinTypes

/**
 * Created by elephant on 2019-11-07 11:44.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易记录的ViewObject
 */
data class TransactionRecordVO(
    val id: Int,
    val coinType: CoinTypes,
    @TransactionType
    val transactionType: Int,
    @TransactionState
    val transactionState: Int,
    val time: Long,
    val address: String,
    val amount: String,
    val gas: String,
    val url: String? = null
)