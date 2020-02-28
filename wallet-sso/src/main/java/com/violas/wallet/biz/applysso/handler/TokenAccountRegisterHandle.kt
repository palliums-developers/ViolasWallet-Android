package com.violas.wallet.biz.applysso.handler

import org.palliums.violascore.wallet.Account

class TokenAccountRegisterHandle(
    account: Account,
    receiveAddress: ByteArray
) : ApplyHandle {
    override fun handler(): Boolean {
        return false
    }
}