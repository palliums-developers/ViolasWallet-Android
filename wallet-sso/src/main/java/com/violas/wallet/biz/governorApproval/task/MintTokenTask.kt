package com.violas.wallet.biz.governorApproval.task

import com.violas.wallet.biz.governorApproval.GovernorApprovalStatus
import org.palliums.violascore.wallet.Account

/**
 * 州长给发行商铸稳定币的任务
 */
class MintTokenTask(
    private val account: Account,
    private val walletAddress: String,
    private val ssoApplicationId: String,
    private val ssoWalletAddress: String,
    private val ssoApplyAmount: Long,
    private val tokenIdx: Long
) : ApprovalTask() {

    override suspend fun handle() {
        getServiceProvider()!!.getTokenManager()
            .mintToken(
                account,
                tokenIdx,
                ssoWalletAddress,
                ssoApplyAmount
            )

        getServiceProvider()!!.getSSOApplicationRecordStorage()
            .updateRecordStatus(
                walletAddress,
                ssoApplicationId,
                GovernorApprovalStatus.MINTED
            )
    }
}