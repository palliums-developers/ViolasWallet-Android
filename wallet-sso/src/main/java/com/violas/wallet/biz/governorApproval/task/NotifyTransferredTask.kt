package com.violas.wallet.biz.governorApproval.task

import com.violas.wallet.biz.SSOApplicationState
import com.violas.wallet.biz.governorApproval.GovernorApprovalStatus

/**
 * 通知已转平台币给发行商的任务
 */
class NotifyTransferredTask(
    private val walletAddress: String,
    private val ssoApplicationId: String,
    private val ssoWalletAddress: String
) : ApprovalTask() {

    override suspend fun handle() {
        getServiceProvider()!!.getGovernorService()
            .submitSSOApplicationApprovalResults(
                ssoApplicationId,
                ssoWalletAddress,
                SSOApplicationState.GOVERNOR_TRANSFERRED
            )

        getServiceProvider()!!.getSSOApplicationRecordStorage()
            .updateRecordStatus(
                walletAddress,
                ssoApplicationId,
                GovernorApprovalStatus.NOTIFIED
            )
    }
}