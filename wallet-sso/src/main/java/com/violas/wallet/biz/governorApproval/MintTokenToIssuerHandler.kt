package com.violas.wallet.biz.governorApproval

import com.violas.wallet.biz.governorApproval.task.MintTokenTask
import com.violas.wallet.biz.governorApproval.task.PublishContractTask
import com.violas.wallet.biz.governorApproval.task.NotifyMintedTask
import org.palliums.violascore.wallet.Account

/**
 * 基于 SSO 相关信息，恢复派生账户。
 *
 * 主账户给SSO申请账户铸币
 * 本地记录主账户已经铸币成功
 *
 * 通知服务器铸币成功通过
 * 删除本地记录主账户的铸币记录
 */
class MintTokenToIssuerHandler(
    private val account: Account,
    private val ssoApplicationId: String,
    private val ssoWalletAddress: String,
    private val ssoApplyAmount: Long,
    private val newTokenIdx: Long
) {

    suspend fun exec() {
        val applyEngine = ApprovalEngine()
        val walletAddress = account.getAddress().toHex()

        val unDoneRecord = applyEngine.getUnMintRecord(
            walletAddress,
            ssoWalletAddress
        )

        applyEngine.addApprovalTask(
            PublishContractTask(
                account,
                walletAddress,
                ssoApplicationId
            )
        )

        applyEngine.addApprovalTask(
            MintTokenTask(
                account,
                walletAddress,
                ssoApplicationId,
                ssoWalletAddress,
                ssoApplyAmount,
                newTokenIdx
            )
        )

        applyEngine.addApprovalTask(
            NotifyMintedTask(
                walletAddress,
                ssoApplicationId,
                ssoWalletAddress
            )
        )

        applyEngine.execMint(unDoneRecord?.status)
    }
}