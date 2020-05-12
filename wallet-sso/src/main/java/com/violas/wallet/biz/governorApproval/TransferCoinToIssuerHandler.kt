package com.violas.wallet.biz.governorApproval

import com.violas.wallet.biz.governorApproval.task.NotifyTransferredTask
import com.violas.wallet.biz.governorApproval.task.TransferCoinTask
import org.palliums.violascore.wallet.Account

/**
 * 转平台币给发行商流程
 *
 * 董事长创建token成功后，主账户向发行商转平台币
 * 本地记录主账户 已转账，记录SSO申请信息
 *
 * 通知服务器 已转账
 * 本地记录主账户 已通知
 */
class TransferCoinToIssuerHandler(
    private val account: Account? = null,
    private val walletAddress: String,
    private val ssoApplicationId: String,
    private val ssoWalletAddress: String
) {

    suspend fun exec() {
        val applyEngine = ApprovalEngine()
        //val walletAddress = account.getAddress().toHex()

        val unDoneRecord =
            applyEngine.getUnTransferRecord(walletAddress, ssoWalletAddress)

        applyEngine.addApprovalTask(
            TransferCoinTask(
                account,
                walletAddress,
                ssoApplicationId,
                ssoWalletAddress,
                100 * 1000_000
            )
        )

        applyEngine.addApprovalTask(
            NotifyTransferredTask(
                walletAddress,
                ssoApplicationId,
                ssoWalletAddress
            )
        )

        applyEngine.execTransfer(unDoneRecord?.status)
    }
}