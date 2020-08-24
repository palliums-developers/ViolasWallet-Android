package com.violas.wallet.repository.http.bank

import androidx.annotation.Keep

/**
 * Created by elephant on 2020/8/24 11:23.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

@Keep
data class CoinCurrDepositDTO(
    val coinName: String,
    val coinModule: String,
    val coinAddress: String,
    val coinLogo: String,
    val principal: String,           // 本金
    val totalEarnings: String,       // 累计收益
    val sevenDayAnnualYield: String  // 7日年化收益率
)