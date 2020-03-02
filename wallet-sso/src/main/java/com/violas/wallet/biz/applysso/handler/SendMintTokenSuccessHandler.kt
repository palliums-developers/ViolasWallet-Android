package com.violas.wallet.biz.applysso.handler

import kotlinx.coroutines.runBlocking

class SendMintTokenSuccessHandler(
    private val accountId: Long,
    private val accountAddress: String,
    private val layerWallet: Long,
    private val ssoApplyAddress: String
) : ApplyHandle() {

    override fun handler(): Boolean {
        return runBlocking {
            try {
                getServiceProvider()!!.getGovernorService()
                    .changeSSOApplicationToMinted(ssoApplyAddress)
                getServiceProvider()!!.getApplySsoRecordDao()
                    .remove(
                        accountId,
                        accountAddress,
                        layerWallet
                    )
                return@runBlocking true
            } catch (e: Exception) {
                e.printStackTrace()
                return@runBlocking false
            }
        }
    }
}