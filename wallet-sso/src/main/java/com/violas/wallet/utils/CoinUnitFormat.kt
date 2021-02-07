package com.violas.wallet.utils

import com.quincysx.crypto.CoinType
import java.math.BigDecimal
import java.math.RoundingMode

fun getCoinDecimal(coinNumber: Int): Long {
    return when (coinNumber) {
        CoinType.Diem.coinNumber(),
        CoinType.Violas.coinNumber() -> {
            1000000
        }
        CoinType.Bitcoin.coinNumber(),
        CoinType.BitcoinTest.coinNumber() -> {
            100000000
        }
        else -> {
            1000000
        }
    }
}


fun convertViolasTokenUnit(amount: Long): String {
    return convertViolasTokenUnit(amount.toString())
}

fun convertViolasTokenUnit(amount: String): String {
    val amountBigDecimal = BigDecimal(amount)
    return if (amountBigDecimal > BigDecimal("0")) {
        amountBigDecimal
            .divide(BigDecimal(1000000), 6, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    } else {
        "0"
    }
}

fun convertDisplayUnitToAmount(amount: String, coinType: CoinType): Long {
    return convertDisplayUnitToAmount(amount.toDouble(), coinType)
}

fun convertDisplayUnitToAmount(amount: Double, coinType: CoinType): Long {
    val amountBigDecimal = BigDecimal(amount.toString())
    val unitBigDecimal = getCoinDecimal(coinType.coinNumber())
    return amountBigDecimal
        .multiply(BigDecimal(unitBigDecimal))
        .toLong()
}

fun convertAmountToDisplayUnit(amount: Long, coinType: CoinType): Pair<String, String> {
    return convertAmountToDisplayUnit(amount.toString(), coinType)
}

fun convertAmountToDisplayUnit(amount: String, coinType: CoinType): Pair<String, String> {
    val amountBigDecimal = BigDecimal(amount)
    val scale: Int
    val unitBigDecimal = when (coinType) {
        CoinType.Violas,
        CoinType.Diem -> {
            scale = 6
            BigDecimal("1000000")
        }
        CoinType.Bitcoin,
        CoinType.BitcoinTest -> {
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
            .divide(unitBigDecimal, scale, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    } else {
        "0"
    }
    return Pair(amountStr, coinType.coinUnit())
}