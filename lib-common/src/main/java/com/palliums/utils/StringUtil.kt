package com.palliums.utils

import java.math.BigDecimal

fun String.stripTrailingZeros(): String {
    if (this.isEmpty()) {
        return this
    }
    return try {
        var amount = this.toDouble()
        if(amount > Double.MAX_VALUE){
            amount = Double.MAX_VALUE
        }

        BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString()
    } catch (e: Exception) {
        this
    }
}