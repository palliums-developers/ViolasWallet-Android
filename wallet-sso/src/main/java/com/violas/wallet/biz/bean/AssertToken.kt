package com.violas.wallet.biz.bean

import com.quincysx.crypto.CoinType
import com.violas.wallet.R

data class AssertToken(
    var fullName: String = "VToken",
    var isToken: Boolean = true,
    var id: Long = 0,
    var account_id: Long = 0,
    var coinType: Int = CoinType.Violas.coinNumber(),
    var tokenIdx: Long = -1,
    var name: String = "Libra",
    var enable: Boolean = false,
    var amount: Long = 0,
    var logo: Int = R.drawable.icon_violas_big
)
