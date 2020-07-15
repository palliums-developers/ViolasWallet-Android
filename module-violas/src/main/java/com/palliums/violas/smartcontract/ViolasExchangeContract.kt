package com.palliums.violas.smartcontract

import com.palliums.violas.smartcontract.violasExchange.AbsViolasExchangeContract

class ViolasExchangeContract(private val testNet: Boolean) : AbsViolasExchangeContract() {
    override fun getContractAddress(): String {
        return if (testNet) {
            "00000000000000000000000000000001"
        } else {
            "00000000000000000000000000000001"
        }
    }
}