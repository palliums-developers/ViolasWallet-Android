package com.violas.wallet.biz.governorApproval.task

import com.violas.wallet.biz.governorApproval.GovernorApprovalStatus
import com.violas.wallet.repository.database.entity.SSOApplicationRecordDo
import org.palliums.violascore.wallet.Account

/**
 * 州长给发行商转平台币的任务
 */
class TransferCoinTask(
    private val account: Account? = null,
    private val walletAddress: String,
    private val ssoApplicationId: String,
    private val ssoWalletAddress: String,
    private val amount: Long
) : ApprovalTask() {

    override suspend fun handle() {
        /*getServiceProvider()!!.getTokenManager().mViolasService
            .sendCoin(
                ContextProvider.getContext(),
                account,
                ssoWalletAddress,
                amount
            )*/

        getServiceProvider()!!.getSSOApplicationRecordStorage()
            .insert(
                SSOApplicationRecordDo(
                    walletAddress = walletAddress,
                    applicationId = ssoApplicationId,
                    status = GovernorApprovalStatus.TRANSFERRED
                )
            )
    }
}