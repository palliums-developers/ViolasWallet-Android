package com.violas.wallet.utils

import com.quincysx.crypto.CoinTypes
import java.math.BigDecimal
import java.math.RoundingMode

fun getCoinDecimal(coinNumber: Int): Long {
    return when (coinNumber) {
        CoinTypes.Libra.coinType(),
        CoinTypes.VToken.coinType() -> {
            1000000
        }
        CoinTypes.Bitcoin.coinType(),
        CoinTypes.BitcoinTest.coinType() -> {
            100000000
        }
        else -> {
            1000000
        }
    }
}

fun convertViolasTokenUnit(amount: String): String {
    val amountBigDecimal = BigDecimal(amount)
    return if (amountBigDecimal > BigDecimal("0")) {
        amountBigDecimal
            .divide(BigDecimal(1000000), 2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    } else {
        "0.00"
    }
}

fun convertDisplayUnitToAmount(amount: String, coinTypes: CoinTypes): Long {
    return convertDisplayUnitToAmount(amount.toDouble(), coinTypes)
}

fun convertDisplayUnitToAmount(amount: Double, coinTypes: CoinTypes): Long {
    val amountBigDecimal = BigDecimal(amount.toString())
    val unitBigDecimal = getCoinDecimal(coinTypes.coinType())
    return amountBigDecimal
        .multiply(BigDecimal(unitBigDecimal))
        .toLong()
}

fun convertAmountToDisplayUnit(amount: Long, coinTypes: CoinTypes): Pair<String, String> {
    return convertAmountToDisplayUnit(amount.toString(), coinTypes)
}

fun convertAmountToDisplayUnit(amount: String, coinTypes: CoinTypes): Pair<String, String> {
    val amountBigDecimal = BigDecimal(amount)
    val scale: Int
    val unitBigDecimal = when (coinTypes) {
        CoinTypes.VToken,
        CoinTypes.Libra -> {
            scale = 6
            BigDecimal("1000000")
        }
        CoinTypes.Bitcoin,
        CoinTypes.BitcoinTest -> {
            scale = 8
            BigDecimal("100000000")
        }
        else -> {
            scale = 8
            BigDecimal("100000000")
        }
    }
    val amountStr = if (amountBigDecimal > BigDecimal("0")) {
        amountBigDecimal
            .divide(unitBigDecimal, scale, RoundingMode.HALF_DOWN)
            .stripTrailingZeros()
            .toPlainString()
    } else {
        "0.00"
    }
    return Pair(amountStr, coinTypes.coinUnit())
}