package com.palliums.violas.smartcontract

import com.palliums.violas.smartcontract.bank.AbsViolasBankContract

class ViolasBankContract(private val testNet: Boolean) : AbsViolasBankContract() {
    override fun getContractAddress(): String {
        return if (testNet) {
            "00000000000000000000000000000001"
        } else {
            "00000000000000000000000000000001"
        }
    }
}