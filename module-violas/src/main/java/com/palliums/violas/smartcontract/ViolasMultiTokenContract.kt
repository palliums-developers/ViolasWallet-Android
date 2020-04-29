package com.palliums.violas.smartcontract

import com.palliums.violas.smartcontract.multitoken.MultiTokenContract

class ViolasMultiTokenContract(private val testNet: Boolean) : MultiTokenContract() {
    override fun getContractAddress(): String {
        return if (testNet) {
            "e1be1ab8360a35a0259f1c93e3eac736"
        } else {
            "e1be1ab8360a35a0259f1c93e3eac736"
        }
    }
}