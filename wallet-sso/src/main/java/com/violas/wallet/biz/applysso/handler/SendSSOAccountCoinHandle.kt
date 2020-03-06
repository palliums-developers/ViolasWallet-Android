package com.violas.wallet.biz.applysso.handler

import com.violas.wallet.biz.applysso.SSOApplyTokenHandler
import org.palliums.violascore.wallet.Account

class SendSSOAccountCoinHandle(
    private val walletAddress: String,
    private val layerWallet: Long,
    private val account: Account,
    private val receiveAddress: String,
    private val amount: Long,
    private val mintTokenAddress: String
) : ApplyHandle() {
    override fun handler(): Boolean {
        try {
            val send = SendAccountCoinHandler(account, receiveAddress, amount).send()
            if (send) {
                getServiceProvider()!!.getApplySsoRecordDao()
                    .updateRecordStatusAndTokenAddress(
                        walletAddress,
                        layerWallet,
                        mintTokenAddress,
                        receiveAddress,
                        SSOApplyTokenHandler.ReadyApproval
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