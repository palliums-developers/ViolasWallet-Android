package com.violas.wallet.biz.bean

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R

data class AssertToken(
    var fullName: String = "VToken",
    var isToken: Boolean = true,
    var id: Long = 0,
    var account_id: Long = 0,
    var coinType: Int = CoinTypes.Violas.coinType(),
    var tokenAddress: String = "",
    var name: String = "Libra",
    var enable: Boolean = false,
    var amount: Long = 0,
    var logo: Int = R.drawable.icon_violas_big,
    var netEnable: Boolean = false
)
