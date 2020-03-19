package com.violas.wallet.biz.applysso.handler

import com.violas.wallet.biz.applysso.SSOApplyTokenHandler
import kotlinx.coroutines.runBlocking

class SendApplySSOHandle(
    private val accountAddress: String,
    private val layerWallet: Long,
    private val mintTokenAddress: String,
    private val ssoApplyWalletAddress: String,
    private val ssoApplicationId: String,
    private val pass: Boolean = true
) : ApplyHandle() {
    override fun handler(): Boolean {
        return runBlocking {
            //todo 拒接审批的逻辑
            try {
                getServiceProvider()!!.getGovernorService()
                    .approvalSSOApplication(
                        ssoApplicationId,
                        ssoApplyWalletAddress,
                        mintTokenAddress,
                        layerWallet,
                        pass
                    )
                if (pass) {
                    getServiceProvider()?.getApplySsoRecordDao()?.updateRecordStatus(
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