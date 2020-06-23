package com.violas.wallet.biz.bean

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R

data class TokenMark(
    val module: String,
    val address: String,
    val name: String
) {
    override fun hashCode(): Int {
        var result = module.hashCode()
        result = result * 31 + address.hashCode()
        result = result * 31 + name.hashCode()
        return result
    }

    override fun toString(): String {
        return module + "   " + address + "     " + name
    }
}

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
