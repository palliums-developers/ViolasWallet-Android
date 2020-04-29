package com.violas.wallet.biz.governorApproval.task

import com.violas.wallet.biz.governorApproval.GovernorApprovalStatus

class SendApproveIssueTokenTask(
    private val walletAddress: String,
    private val ssoApplicationId: String,
    private val ssoWalletAddress: String
) : ApprovalTask() {

    override suspend fun handle() {
        getServiceProvider()!!.getGovernorService()
            .approveSSOApplication(
                ssoApplicationId,
                ssoWalletAddress
            )

        getServiceProvider()!!.getApplySsoRecordDao()
            .updateRecordStatus(
                walletAddress,
                ssoApplicationId,
                GovernorApprovalStatus.Approval
            )
    }
}