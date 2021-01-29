package com.violas.wallet.utils

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.Vm
import java.util.*

fun str2CoinNumber(str: String): Int? {
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

fun str2CoinType(chainName: String?): CoinTypes {
    return when {
        chainName?.equals("btc", true) == true
                || chainName?.equals("bitcoin", true) == true ->
            if (Vm.TestNet) CoinTypes.BitcoinTest else CoinTypes.Bitcoin
        chainName?.equals("libra", true) == true ->
            CoinTypes.Libra
        else ->
            CoinTypes.Violas
    }
}

fun getViolasCoinType(): CoinTypes {
    return if (Vm.TestNet) CoinTypes.Violas else CoinTypes.Violas
}

fun getLibraCoinType(): CoinTypes {
    return if (Vm.TestNet) CoinTypes.Libra else CoinTypes.Libra
}

fun getBtcCoinType(): CoinTypes {
    return if (Vm.TestNet) CoinTypes.BitcoinTest else CoinTypes.Bitcoin
}
