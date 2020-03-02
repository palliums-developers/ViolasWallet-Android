package com.violas.wallet.biz.applysso.handler

import com.violas.wallet.biz.applysso.SSOApplyTokenHandler
import kotlinx.coroutines.runBlocking

class SendApplySSOHandle(
    private val accountId: Long,
    private val accountAddress: String,
    private val layerWallet: Long,
    private val mintTokenAddress: String,
    private val SSOApplyWalletAddress: String,
    private val pass: Boolean = false
) : ApplyHandle() {
    override fun handler(): Boolean {
        return runBlocking {
            //todo 拒接审批的逻辑
            try {
                getServiceProvider()!!.getGovernorService()
                    .approvalSSOApplication(
                        pass,
                        mintTokenAddress,
                        SSOApplyWalletAddress,
                        layerWallet
                    )
                if (!pass) {
                    getServiceProvider()?.getApplySsoRecordDao()?.updateRecordStatus(
                        accountId,
                        accountAddress,
                        layerWallet,
                        SSOApplyTokenHandler.Approval
                    )
                }
                return@runBlocking true
            } catch (e: Exception) {
                e.printStackTrace()
                return@runBlocking false
            }
        }
    }
}