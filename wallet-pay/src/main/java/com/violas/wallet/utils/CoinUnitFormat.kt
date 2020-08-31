package com.violas.wallet.utils

import com.quincysx.crypto.CoinTypes
import java.math.BigDecimal
import java.math.RoundingMode

fun getCoinUnit(coinType: CoinTypes): Long {
    return when (coinType) {
        CoinTypes.Libra,
        CoinTypes.Violas -> {
            1000000
        }
        CoinTypes.Bitcoin,
        CoinTypes.BitcoinTest -> {
            100000000
        }
        else -> {
            1000000
        }
    }
}

fun getCoinDecimal(coinType: CoinTypes): Int {
    return when (coinType) {
        CoinTypes.Libra,
        CoinTypes.Violas -> {
            6
        }
        CoinTypes.Bitcoin,
        CoinTypes.BitcoinTest -> {
            8
        }
        else -> {
            8
        }
    }
}

fun convertViolasTokenUnit(amount: Long): String {
    return convertViolasTokenUnit(amount.toString())
}

fun convertViolasTokenUnit(amount: String): String {
    val amountBigDecimal = BigDecimal(amount)
    return if (amountBigDecimal > BigDecimal.ZERO) {
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
    return convertDisplayUnitToAmount(amount.toDoubleOrNull() ?: 0.0, coinTypes)
}

fun convertDisplayUnitToAmount(amount: Double, coinTypes: CoinTypes): Long {
    val amountBigDecimal = BigDecimal(amount.toString())
    val unitBigDecimal = getCoinUnit(coinTypes)
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
    val amountStr = if (amountBigDecimal > BigDecimal.ZERO) {
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
        .multiply(BigDecimal(getCoinUnit(coinTypes)))
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
    return if (amountBigDecimal > BigDecimal.ZERO)
        amountBigDecimal
            .divide(
                BigDecimal(getCoinUnit(coinTypes)),
                getCoinDecimal(coinTypes),
                RoundingMode.HALF_UP
            )
            .stripTrailingZeros()
    else
        BigDecimal.ZERO.stripTrailingZeros()
}

fun convertAmountToDisplayAmountStr(
    amount: Long,
    coinTypes: CoinTypes = CoinTypes.Violas
): String {
    return convertAmountToDisplayAmount(
        BigDecimal(amount),
        coinTypes
    ).toPlainString()
}

fun convertAmountToDisplayAmountStr(
    amountStr: String,
    coinTypes: CoinTypes = CoinTypes.Violas
): String {
    return convertAmountToDisplayAmount(
        BigDecimal(amountStr),
        coinTypes
    ).toPlainString()
}

fun convertAmountToDisplayAmountStr(
    amountBigDecimal: BigDecimal,
    coinTypes: CoinTypes = CoinTypes.Violas
): String {
    return convertAmountToDisplayAmount(
        amountBigDecimal,
        coinTypes
    ).toPlainString()
}

fun convertAmountToExchangeRate(
    amountA: Long,
    amountB: Long,
    coinTypesB: CoinTypes = CoinTypes.Violas
): BigDecimal? {
    return convertAmountToExchangeRate(
        BigDecimal(amountA),
        BigDecimal(amountB),
        coinTypesB
    )
}

fun convertAmountToExchangeRate(
    amountAStr: String,
    amountBStr: String,
    coinTypesB: CoinTypes = CoinTypes.Violas
): BigDecimal? {
    return convertAmountToExchangeRate(
        BigDecimal(amountAStr),
        BigDecimal(amountBStr),
        coinTypesB
    )
}

fun convertAmountToExchangeRate(
    amountABigDecimal: BigDecimal,
    amountBBigDecimal: BigDecimal,
    coinTypesB: CoinTypes = CoinTypes.Violas
): BigDecimal? {
    return if (amountABigDecimal > BigDecimal.ZERO && amountBBigDecimal > BigDecimal.ZERO)
        amountBBigDecimal
            .divide(
                amountABigDecimal,
                getCoinDecimal(coinTypesB),
                RoundingMode.DOWN
            )
            .stripTrailingZeros()
    else
        null
}

fun getAmountPrefix(amountBigDecimal: BigDecimal, input: Boolean): String {
    return when {
        amountBigDecimal <= BigDecimal.ZERO -> ""
        input -> "+ "
        else -> "- "
    }
}

fun keepTwoDecimals(amountStr: String): String {
    return BigDecimal(amountStr)
        .setScale(2, RoundingMode.DOWN)
        .toPlainString()
}