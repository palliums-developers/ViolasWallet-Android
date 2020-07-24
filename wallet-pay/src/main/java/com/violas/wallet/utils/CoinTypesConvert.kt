package com.violas.wallet.utils

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.Vm
import java.util.*

fun str2CoinType(str: String): Int? {
    return when (str.toLowerCase(Locale.ROOT)) {
        "btc" -> {
            if (Vm.TestNet) {
                CoinTypes.BitcoinTest.coinType()
            } else {
                CoinTypes.Bitcoin.coinType()
            }
        }
        "libra" -> {
            CoinTypes.Libra.coinType()
        }
        "violas" -> {
            CoinTypes.Violas.coinType()
        }
        else -> null
    }
}