package com.violas.wallet.biz.applysso.handler

import org.palliums.violascore.wallet.Account

class SendAccountCoinHandler(
    account: Account,
    receiveAddress: ByteArray,
    amount: Int
) {
    fun send(): Boolean {
        return false
    }
}