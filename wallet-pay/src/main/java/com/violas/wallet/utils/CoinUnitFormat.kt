package com.violas.wallet.utils

import com.quincysx.crypto.CoinTypes
import java.math.BigDecimal
import java.math.RoundingMode

val ZERO_BIGDECIMAL by lazy {
    BigDecimal("0")
}

fun getCoinDecimal(coinNumber: Int): Long {
    return when (coinNumber) {
        CoinTypes.Libra.coinType(),
        CoinTypes.Violas.coinType() -> {
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

fun convertViolasTokenUnit(amount: Long): String {
    return convertViolasTokenUnit(amount.toString())
}

fun convertViolasTokenUnit(amount: String): String {
    val amountBigDecimal = BigDecimal(amount)
    return if (amountBigDecimal > ZERO_BIGDECIMAL) {
        amountBigDecimal
            .divide(BigDecimal(1000000), 6, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    } else {
        "0"
    }
}

fun convertViolasTokenPrice(price: String): String {
    return BigDecimal(price).stripTrailingZeros().toPlainString()
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
        CoinTypes.Violas,
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
    val amountStr = if (amountBigDecimal > ZERO_BIGDECIMAL) {
        amountBigDecimal
            .divide(unitBigDecimal, scale, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    } else {
        "0"
    }
    return Pair(amountStr, coinTypes.coinUnit())
}

fun convertDisplayAmountToAmount(
    amountStr: String,
    coinTypes: CoinTypes = CoinTypes.Violas
): BigDecimal {
    return convertDisplayAmountToAmount(BigDecimal(amountStr), coinTypes)
}

fun convertDisplayAmountToAmount(
    amountBigDecimal: BigDecimal,
    coinTypes: CoinTypes = CoinTypes.Violas
): BigDecimal {
    return amountBigDecimal
        .multiply(BigDecimal(getCoinDecimal(coinTypes.coinType())))
        .stripTrailingZeros()
}

fun convertAmountToDisplayAmount(
    amount: Long,
    coinTypes: CoinTypes = CoinTypes.Violas
): BigDecimal {
    return convertAmountToDisplayAmount(BigDecimal(amount), coinTypes)
}

fun convertAmountToDisplayAmount(
    amountStr: String,
    coinTypes: CoinTypes = CoinTypes.Violas
): BigDecimal {
    return convertAmountToDisplayAmount(BigDecimal(amountStr), coinTypes)
}

fun convertAmountToDisplayAmount(
    amountBigDecimal: BigDecimal,
    coinTypes: CoinTypes = CoinTypes.Violas
): BigDecimal {
    val scale: Int
    val unitBigDecimal = when (coinTypes) {
        CoinTypes.Violas,
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
    return if (amountBigDecimal > ZERO_BIGDECIMAL)
        amountBigDecimal
            .divide(unitBigDecimal, scale, RoundingMode.HALF_UP)
            .stripTrailingZeros()
    else
        ZERO_BIGDECIMAL
}

fun convertAmountToDisplayAmountStr(
    amount: Long,
    coinTypes: CoinTypes = CoinTypes.Violas
): String {
    return convertAmountToDisplayAmount(BigDecimal(amount), coinTypes).toPlainString()
}

fun convertAmountToDisplayAmountStr(
    amountStr: String,
    coinTypes: CoinTypes = CoinTypes.Violas
): String {
    return convertAmountToDisplayAmount(BigDecimal(amountStr), coinTypes).toPlainString()
}

fun convertAmountToDisplayAmountStr(
    amountBigDecimal: BigDecimal,
    coinTypes: CoinTypes = CoinTypes.Violas
): String {
    return convertAmountToDisplayAmount(amountBigDecimal, coinTypes).toPlainString()
}

fun convertAmountToExchangeRate(
    amountA: Long,
    amountB: Long
): BigDecimal {
    return convertAmountToExchangeRate(BigDecimal(amountA), BigDecimal(amountB))
}

fun convertAmountToExchangeRate(
    amountAStr: String,
    amountBStr: String
): BigDecimal {
    return convertAmountToExchangeRate(BigDecimal(amountAStr), BigDecimal(amountBStr))
}

fun convertAmountToExchangeRate(
    amountABigDecimal: BigDecimal,
    amountBBigDecimal: BigDecimal
): BigDecimal {
    return amountBBigDecimal
        .divide(amountABigDecimal, 8, RoundingMode.DOWN)
        .stripTrailingZeros()
}

fun convertAmountToExchangeRateStr(
    amountA: Long,
    amountB: Long
): String {
    return convertAmountToExchangeRate(BigDecimal(amountA), BigDecimal(amountB)).toPlainString()
}

fun convertAmountToExchangeRateStr(
    amountAStr: String,
    amountBStr: String
): String {
    return convertAmountToExchangeRate(BigDecimal(amountAStr), BigDecimal(amountBStr))
        .toPlainString()
}

fun convertAmountToExchangeRateStr(
    amountABigDecimal: BigDecimal,
    amountBBigDecimal: BigDecimal
): String {
    return convertAmountToExchangeRate(amountABigDecimal, amountBBigDecimal).toPlainString()
}