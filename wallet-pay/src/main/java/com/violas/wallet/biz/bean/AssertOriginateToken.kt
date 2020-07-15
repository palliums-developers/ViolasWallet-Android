package com.violas.wallet.biz.bean

import com.palliums.violas.bean.TokenMark
import com.quincysx.crypto.CoinTypes

data class AssertOriginateToken(
    var tokenMark: TokenMark? = null,
    var fullName: String = "VToken",
    var isToken: Boolean = true,
    var id: Long = 0,
    var account_id: Long = 0,
    var coinType: Int = CoinTypes.Violas.coinType(),
    var name: String = "Libra",
    var enable: Boolean = false,
    var amount: Long = 0,
    var logo: String = "",
    var netEnable: Boolean = true
)
