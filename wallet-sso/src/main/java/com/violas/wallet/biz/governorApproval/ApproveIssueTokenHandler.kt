package com.violas.wallet.biz.governorApproval

import com.violas.wallet.biz.governorApproval.task.SendApproveIssueTokenTask
import com.violas.wallet.biz.governorApproval.task.TransferCoinToSSOTask
import org.palliums.violascore.wallet.Account

/**
 * 申请审批流程
 *
 * 铸币账户注册稳定币
 * 本地记录某一层铸币账户已经注册稳定币成功
 *
 * 董事长创建token成功后，主账户向 SSO 申请账户打 vToken
 * 本地记录某一层铸币账户已经准备审批，记录 SSO 申请账户的信息
 *
 * 通知服务器审批申请通过
 * 本地记录主账户已经审批成功
 */
class ApproveSSOIssueTokenHandler(
    private val account: Account? = null,
    private val walletAddress: String,
    private val ssoApplicationId: String,
    private val ssoWalletAddress: String
) {

    suspend fun exec() {
        val applyEngine = ApprovalEngine()
        //val walletAddress = account.getAddress().toHex()

        val findUnDoneRecord =
            applyEngine.getUnApproveRecord(walletAddress, ssoWalletAddress)

        applyEngine.addApplyHandle(
            TransferCoinToSSOTask(
                account,
                walletAddress,
                ssoApplicationId,
                ssoWalletAddress,
                100 * 1000_000
            )
        )

        applyEngine.addApplyHandle(
            SendApproveIssueTokenTask(
                walletAddress,
                ssoApplicationId,
                ssoWalletAddress
            )
        )

        applyEngine.execSSOApply(findUnDoneRecord?.status)
    }
}