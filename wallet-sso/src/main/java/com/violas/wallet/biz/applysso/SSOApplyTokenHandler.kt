package com.violas.wallet.biz.applysso

import androidx.annotation.IntDef
import androidx.annotation.WorkerThread
import com.violas.wallet.biz.applysso.handler.SendApplySSOHandle
import com.violas.wallet.biz.applysso.handler.SendSSOAccountCoinHandle
import org.palliums.violascore.wallet.Account

@IntDef(
    SSOApplyTokenHandler.None,
    SSOApplyTokenHandler.ReadyApproval,
    SSOApplyTokenHandler.Approval,
    SSOApplyTokenHandler.PublishSuccess,
    SSOApplyTokenHandler.MintSuccess
)
annotation class SSOApplyTokenStatus

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
class SSOApplyTokenHandler(
    private val account: Account,
    private val ssoWalletAddress: String,
    private val ssoApplicationId: String,
    private val newTokenIdx: Long
) {
    companion object {
        /**
         * 没有任何准备
         */
        const val None = 0

        /**
         * 已经可以审批 SSO 申请
         */
        const val ReadyApproval = 1

        /**
         * 审批 SSO 申请完成
         */
        const val Approval = 2

        /**
         * publish 合约成功
         */
        const val PublishSuccess = 3

        /**
         * Mint 币成功
         */
        const val MintSuccess = 4
    }

    @WorkerThread
    suspend fun exec() {
        val applyEngine = ApplyEngine()
        val walletAddress = account.getAddress().toHex()

        val findUnDoneRecord =
            applyEngine.getUnDoneRecord(walletAddress, ssoWalletAddress)

        applyEngine.addApplyHandle(
            SendSSOAccountCoinHandle(
                walletAddress,
                account,
                ssoWalletAddress,
                ssoApplicationId,
                newTokenIdx,
                100 * 1000_000
            )
        )
        applyEngine.addApplyHandle(
            SendApplySSOHandle(
                walletAddress,
                ssoWalletAddress,
                ssoApplicationId,
                newTokenIdx
            )
        )

        applyEngine.execSSOApply(findUnDoneRecord?.status)
    }
}