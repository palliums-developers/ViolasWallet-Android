package com.violas.wallet.utils

import com.quincysx.crypto.CoinType
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import java.util.*

fun str2CoinNumber(str: String): Int? {
    return when (str.toLowerCase(Locale.ROOT)) {
        "btc" -> {
            getBitcoinCoinType().coinNumber()
        }
        "libra" -> {
            getDiemCoinType().coinNumber()
        }
        "violas" -> {
            getViolasCoinType().coinNumber()
        }
        else -> null
    }
}

fun str2CoinType(chainName: String?): CoinType {
    return when {
        chainName?.equals("btc", true) == true
                || chainName?.equals("bitcoin", true) == true ->
            getBitcoinCoinType()
        chainName?.equals("libra", true) == true ->
            getDiemCoinType()
        else ->
            getViolasCoinType()
    }
}
