package com.violas.wallet.repository.http.bank

import androidx.annotation.Keep

/**
 * Created by elephant on 2020/8/24 11:23.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

@Keep
data class CurrDepositDTO(
    val coinName: String,
    val coinModule: String,
    val coinAddress: String,
    val coinLogo: String,
    val principal: String,           // 本金
    val totalEarnings: String,       // 累计收益
    val sevenDayAnnualYield: String  // 7日年化收益率
)

@Keep
data class CurrBorrowingDTO(
    val coinName: String,
    val coinModule: String,
    val coinAddress: String,
    val coinLogo: String,
    val borrowed: String            // 借款金额
)

@Keep
data class DepositRecordDTO(
    val coinName: String,
    val coinLogo: String,
    val amount: String,
    val time: Long,
    val state: Int
)

@Keep
data class BorrowingRecordDTO(
    val coinName: String,
    val coinLogo: String,
    val amount: String,
    val time: Long,
    val state: Int
)