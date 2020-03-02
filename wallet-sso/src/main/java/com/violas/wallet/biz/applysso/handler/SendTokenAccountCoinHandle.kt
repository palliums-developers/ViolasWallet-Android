package com.violas.wallet.biz.applysso.handler

import com.violas.wallet.biz.applysso.SSOApplyTokenHandler
import org.palliums.violascore.wallet.Account

class SendTokenAccountCoinHandle(
    private val accountId: Long,
    private val account: Account,
    private val layerWallet: Long,
    private val receiveAddress: String,
    private val amount: Long
) : ApplyHandle() {
    override fun handler(): Boolean {
        try {
            val send = SendAccountCoinHandler(account, receiveAddress, amount).send()
            if (send) {
                getServiceProvider()!!.getApplySsoRecordDao()
                    .updateRecordStatus(
                        accountId,
                        account.getAddress().toHex(),
                        layerWallet,
                        SSOApplyTokenHandler.ReadyRegister
                    )
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return false
    }
}