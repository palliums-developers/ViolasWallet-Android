package com.violas.wallet.biz.applysso

import androidx.annotation.WorkerThread
import com.violas.wallet.biz.applysso.handler.MintTokenHandler
import com.violas.wallet.biz.applysso.handler.PublishTokenHandler
import com.violas.wallet.biz.applysso.handler.SendMintTokenSuccessHandler
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
class SSOMintTokenHandler(
    private val account: Account,
    private val ssoWalletAddress: String,
    private val ssoApplyAmount: Long,
    private val ssoApplicationId: String,
    private val newTokenIdx: Long
) {

    @WorkerThread
    suspend fun exec() {
        val applyEngine = ApplyEngine()
        val walletAddress = account.getAddress().toHex()

        val findUnDoneRecord = applyEngine.getUnMintRecord(
            walletAddress,
            newTokenIdx,
            ssoWalletAddress
        )

        applyEngine.addApplyHandle(
            PublishTokenHandler(
                walletAddress,
                account,
                ssoApplicationId
            )
        )

        applyEngine.addApplyHandle(
            MintTokenHandler(
                walletAddress,
                account,
                ssoWalletAddress,
                ssoApplyAmount,
                ssoApplicationId,
                newTokenIdx
            )
        )

        applyEngine.addApplyHandle(
            SendMintTokenSuccessHandler(
                walletAddress,
                ssoWalletAddress,
                ssoApplicationId
            )
        )

        applyEngine.execMint(findUnDoneRecord?.status)
    }
}