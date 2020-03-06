package com.violas.wallet.biz.applysso.handler

import com.violas.wallet.biz.applysso.SSOApplyTokenHandler
import kotlinx.coroutines.runBlocking

class SendApplySSOHandle(
    private val accountAddress: String,
    private val layerWallet: Long,
    private val mintTokenAddress: String,
    private val SSOApplyWalletAddress: String
    ) : ApplyHandle() {
    override fun handler(): Boolean {
        return runBlocking {
            try {
                getServiceProvider()!!.getGovernorService()
                    .approvalSSOApplication(
                        true,
                        mintTokenAddress,
                        SSOApplyWalletAddress,
                        layerWallet
                    )

                getServiceProvider()?.getApplySsoRecordDao()?.updateRecordStatus(
                    accountAddress,
                    layerWallet,
                    SSOApplyTokenHandler.Approval
                )
                return@runBlocking true
            } catch (e: Exception) {
                e.printStackTrace()
                return@runBlocking false
            }
        }
    }
}