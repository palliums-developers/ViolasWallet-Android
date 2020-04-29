package com.violas.wallet.biz.governorApproval.task

import com.violas.wallet.biz.governorApproval.GovernorApprovalStatus
import org.palliums.violascore.wallet.Account

class PublishTokenTask(
    private val account: Account,
    private val walletAddress: String,
    private val ssoApplicationId: String
) : ApprovalTask() {

    override suspend fun handle() {
        val tokenManager = getServiceProvider()!!.getTokenManager()
        if (!tokenManager.isPublishedContract(walletAddress)) {
            tokenManager.publishContract(account)
        }

        getServiceProvider()!!.getApplySsoRecordDao()
            .updateRecordStatus(
                walletAddress,
                ssoApplicationId,
                GovernorApprovalStatus.PublishSuccess
            )
    }
}