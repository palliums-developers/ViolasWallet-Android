package com.violas.wallet.biz.governorApproval.task

import com.violas.wallet.biz.SSOApplicationState

/**
 *  通知已铸稳定币给发行商的任务
 */
class NotifyMintedTask(
    private val walletAddress: String,
    private val ssoApplicationId: String,
    private val ssoWalletAddress: String
) : ApprovalTask() {

    override suspend fun handle() {
        getServiceProvider()!!.getGovernorService()
            .submitSSOApplicationApprovalResults(
                ssoApplicationId,
                ssoWalletAddress,
                SSOApplicationState.GOVERNOR_MINTED
            )

        getServiceProvider()!!.getSSOApplicationRecordStorage()
            .remove(
                walletAddress,
                ssoApplicationId
            )
    }
}