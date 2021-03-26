package com.violas.wallet.biz.bean

import com.violas.wallet.common.getViolasCoinType

data class AssertOriginateToken(
    var currency: DiemCurrency? = null,
    var fullName: String = "VToken",
    var isToken: Boolean = true,
    var id: Long = 0,
    var accountId: Long = 0,
    var coinNumber: Int = getViolasCoinType().coinNumber(),
    var name: String = "Libra",
    var enable: Boolean = false,
    var amount: Long = 0,
    var logo: String = "",
    var netEnable: Boolean = true
)
