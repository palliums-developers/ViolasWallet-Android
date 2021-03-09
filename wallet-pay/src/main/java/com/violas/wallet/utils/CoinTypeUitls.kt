package com.violas.wallet.utils

import com.quincysx.crypto.CoinType
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getEthereumCoinType
import com.violas.wallet.common.getViolasCoinType
import java.util.*

fun str2CoinNumber(str: String): Int? {
    return when (str.toLowerCase(Locale.ROOT)) {
        "btc", "bitcoin" -> {
            getBitcoinCoinType().coinNumber()
        }
        "libra", "diem" -> {
            getDiemCoinType().coinNumber()
        }
        "ethereum", "ether", "eth" -> {
            getEthereumCoinType().coinNumber()
        }
        "violas" -> {
            getViolasCoinType().coinNumber()
        }
        else -> null
    }
}

fun str2CoinType(chainName: String?): CoinType {
    return when {
        chainName?.equals("Bitcoin", true) == true
                || chainName?.equals("BTC", true) == true ->
            getBitcoinCoinType()
        chainName?.equals("Diem", true) == true
                || chainName?.equals("Libra", true) == true ->
            getDiemCoinType()
        chainName?.equals("Ethereum", true) == true
                || chainName?.equals("Ether", true) == true
                || chainName?.equals("ETH", true) == true ->
            getEthereumCoinType()
        chainName?.equals("Violas", true) == true ->
            getViolasCoinType()
        else ->
            getViolasCoinType()
    }
}
