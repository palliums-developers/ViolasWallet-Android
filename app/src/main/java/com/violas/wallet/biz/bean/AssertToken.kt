package com.violas.wallet.biz.bean

data class AssertToken(
    var fullName: String = "VToken",
    var isToken: Boolean = true,
    var id: Long = 0,
    var account_id: Long = 0,
    var name: String = "Libra",
    var enable: Boolean = false,
    var amount: Long = 0
)
