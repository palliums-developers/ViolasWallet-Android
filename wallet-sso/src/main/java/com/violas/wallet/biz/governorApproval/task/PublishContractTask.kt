package com.violas.wallet.biz.governorApproval.task

import com.violas.wallet.biz.governorApproval.GovernorApprovalStatus
import org.palliums.violascore.wallet.Account

/**
 * 州长 publish 合约的任务
 */
class PublishContractTask(
    private val account: Account,
    private val walletAddress: String,
    private val ssoApplicationId: String
) : ApprovalTask() {

    override suspend fun handle() {
        val tokenManager = getServiceProvider()!!.getTokenManager()
        if (!tokenManager.isPublishedContract(walletAddress)) {
            tokenManager.publishContract(account)
        }

        getServiceProvider()!!.getSSOApplicationRecordStorage()
            .updateRecordStatus(
                walletAddress,
                ssoApplicationId,
                GovernorApprovalStatus.PUBLISHED
            )
    }
}