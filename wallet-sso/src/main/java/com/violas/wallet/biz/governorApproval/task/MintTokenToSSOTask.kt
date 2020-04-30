package com.violas.wallet.biz.governorApproval.task

import com.violas.wallet.biz.governorApproval.GovernorApprovalStatus
import org.palliums.violascore.wallet.Account

class MintTokenToSSOTask(
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

        getServiceProvider()!!.getApplySsoRecordDao()
            .updateRecordStatus(
                walletAddress,
                ssoApplicationId,
                GovernorApprovalStatus.MintSuccess
            )
    }
}