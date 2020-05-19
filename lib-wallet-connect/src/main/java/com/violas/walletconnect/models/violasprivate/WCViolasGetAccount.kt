package com.violas.walletconnect.models.violasprivate

open class WCViolasGetAccount(
    val wallets: List<WCViolasAccount>
)

open class WCViolasAccount(
    val walletType: Int,
    val coinType: String,
    val name: String,
    val address: String
)