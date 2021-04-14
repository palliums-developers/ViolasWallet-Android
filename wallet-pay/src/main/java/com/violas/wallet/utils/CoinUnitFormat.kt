package com.violas.wallet.utils

import com.quincysx.crypto.CoinType
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import java.math.BigDecimal
import java.math.RoundingMode

fun getCoinUnit(coinType: CoinType): Long {
    return when (coinType) {
        getDiemCoinType(),
        getViolasCoinType() -> {
            1000000
        }
        getBitcoinCoinType() -> {
            100000000
        }
        else -> {
            1000000
        }
    }
}

fun getCoinDecimal(coinType: CoinType): Int {
    return when (coinType) {
        getDiemCoinType(),
        getViolasCoinType() -> {
            6
        }
        getBitcoinCoinType() -> {
            8
        }
        else -> {
            8
        }
    }
}

fun convertDisplayUnitToAmount(amount: String, coinType: CoinType): Long {
    return convertDisplayUnitToAmount(amount.toDoubleOrNull() ?: 0.0, coinType)
}

fun convertDisplayUnitToAmount(amount: Double, coinType: CoinType): Long {
    val amountBigDecimal = BigDecimal(amount.toString())
    val unitBigDecimal = getCoinUnit(coinType)
    return amountBigDecimal
        .multiply(BigDecimal(unitBigDecimal))
        .toLong()
}

fun convertAmountToDisplayUnit(amount: Long, coinType: CoinType): Pair<String, String> {
    return convertAmountToDisplayUnit(BigDecimal(amount), coinType)
}

fun convertAmountToDisplayUnit(amount: String, coinType: CoinType): Pair<String, String> {
    return convertAmountToDisplayUnit(amount.toBigDecimalOrNull() ?: BigDecimal.ZERO, coinType)
}

fun convertAmountToDisplayUnit(amount: BigDecimal, coinType: CoinType): Pair<String, String> {
    val scale: Int
    val unitBigDecimal = when (coinType) {
        getViolasCoinType(),
        getDiemCoinType() -> {
            scale = 6
            BigDecimal("1000000")
        }
        getBitcoinCoinType() -> {
            scale = 8
            BigDecimal("100000000")
        }
        else -> {
            scale = 8
            BigDecimal("100000000")
        }
    }
    val amountStr = if (amount > BigDecimal.ZERO) {
        amount
            .divide(unitBigDecimal, scale, RoundingMode.DOWN)
            .stripTrailingZeros()
            .toPlainString()
    } else {
        "0"
    }
    return Pair(amountStr, coinType.coinUnit())
}

fun convertDisplayAmountToAmount(
    amountStr: String,
    coinType: CoinType = getViolasCoinType()
): BigDecimal {
    return convertDisplayAmountToAmount(BigDecimal(amountStr), coinType)
}

fun convertDisplayAmountToAmount(
    amountBigDecimal: BigDecimal,
    coinType: CoinType = getViolasCoinType()
): BigDecimal {
    return amountBigDecimal
        .multiply(BigDecimal(getCoinUnit(coinType)))
}

fun convertAmountToDisplayAmount(
    amount: Long,
    coinType: CoinType = getViolasCoinType()
): BigDecimal {
    return convertAmountToDisplayAmount(BigDecimal(amount), coinType)
}

fun convertAmountToDisplayAmount(
    amountStr: String,
    coinType: CoinType = getViolasCoinType()
): BigDecimal {
    return convertAmountToDisplayAmount(BigDecimal(amountStr), coinType)
}

fun convertAmountToDisplayAmount(
    amountBigDecimal: BigDecimal,
    coinType: CoinType = getViolasCoinType(),
    decimalScale: Int? = null,
    roundingMode: RoundingMode = RoundingMode.DOWN
): BigDecimal {
    return if (amountBigDecimal > BigDecimal.ZERO)
        amountBigDecimal
            .divide(
                BigDecimal(getCoinUnit(coinType)),
                decimalScale ?: getCoinDecimal(coinType),
                roundingMode
            )
            .stripTrailingZeros()
    else
        BigDecimal.ZERO.stripTrailingZeros()
}

fun convertAmountToDisplayAmountStr(
    amount: Long,
    coinType: CoinType = getViolasCoinType()
): String {
    return convertAmountToDisplayAmount(
        BigDecimal(amount),
        coinType
    ).toPlainString()
}

fun convertAmountToDisplayAmountStr(
    amountStr: String,
    coinType: CoinType = getViolasCoinType()
): String {
    return convertAmountToDisplayAmount(
        BigDecimal(amountStr),
        coinType
    ).toPlainString()
}

fun convertAmountToDisplayAmountStr(
    amountBigDecimal: BigDecimal,
    coinType: CoinType = getViolasCoinType()
): String {
    return convertAmountToDisplayAmount(
        amountBigDecimal,
        coinType
    ).toPlainString()
}

fun convertAmountToExchangeRate(
    amountA: Long,
    amountB: Long,
    coinTypeB: CoinType = getViolasCoinType()
): BigDecimal? {
    return convertAmountToExchangeRate(
        BigDecimal(amountA),
        BigDecimal(amountB),
        coinTypeB
    )
}

fun convertAmountToExchangeRate(
    amountAStr: String,
    amountBStr: String,
    coinTypeB: CoinType = getViolasCoinType()
): BigDecimal? {
    return convertAmountToExchangeRate(
        BigDecimal(amountAStr),
        BigDecimal(amountBStr),
        coinTypeB
    )
}

fun convertAmountToExchangeRate(
    amountABigDecimal: BigDecimal,
    amountBBigDecimal: BigDecimal,
    coinTypeB: CoinType = getViolasCoinType()
): BigDecimal? {
    if (amountABigDecimal > BigDecimal.ZERO && amountBBigDecimal > BigDecimal.ZERO) {
        val rateBigDecimal = amountBBigDecimal.divide(
            amountABigDecimal,
            20,
            RoundingMode.HALF_UP
        ).stripTrailingZeros()

        try {
            val array = rateBigDecimal.toPlainString().split(".")
            if (array.size < 2 || array[1].length <= getCoinDecimal(coinTypeB))
                return rateBigDecimal

            var scaleStart = 0
            val charsStart = array[1].toCharArray()
            for (char in charsStart) {
                scaleStart++
                if (char != '0')
                    break
            }

            var scaleEnd = getCoinDecimal(coinTypeB)
            val charsEnd = array[1].substring(scaleEnd).toCharArray()
            for (char in charsEnd) {
                scaleEnd++
                if (char != '0')
                    break
            }

            val scale = if (scaleStart < scaleEnd)
                getCoinDecimal(coinTypeB)
            else
                scaleEnd + 1

            return amountBBigDecimal.divide(
                amountABigDecimal,
                scale,
                RoundingMode.HALF_UP
            ).stripTrailingZeros()
        } catch (e: Exception) {
            return rateBigDecimal
        }
    } else {
        return null
    }
}

fun getAmountPrefix(amountBigDecimal: BigDecimal, input: Boolean): String {
    return when {
        amountBigDecimal <= BigDecimal.ZERO -> ""
        input -> "+ "
        else -> "- "
    }
}

fun keepTwoDecimal(amount: BigDecimal?): String {
    return (amount ?: BigDecimal.ZERO)
        .setScale(2, RoundingMode.DOWN)
        .toPlainString()
}

fun convertRateToPercentage(rate: Double, minimumDecimalScale: Int = 2): String {
    val rateStr = BigDecimal(rate * 100).toPlainString()
    val array = rateStr.split(".")
    return if (array.size >= 2 && array[1].length > minimumDecimalScale) {
        "$rateStr%"
    } else {
        "${
            BigDecimal(rate * 100)
                .setScale(minimumDecimalScale, RoundingMode.DOWN)
                .toPlainString()
        }%"
    }
}