package com.violas.wallet.biz.applysso.handler

import org.palliums.violascore.wallet.Account

class SendTokenAccountCoinHandle(
    private val account: Account,
    private val receiveAddress: ByteArray,
    private val amount: Int
) : ApplyHandle {
    override fun handler(): Boolean {
        SendAccountCoinHandler(account, receiveAddress, amount).send()
        return false
    }
}