package com.violas.wallet.biz.applysso

import androidx.annotation.IntDef
import androidx.annotation.WorkerThread
import com.violas.wallet.biz.applysso.handler.*
import com.violas.wallet.repository.DataRepository
import org.palliums.violascore.wallet.Account
import org.palliums.violascore.wallet.LibraWallet
import org.palliums.violascore.wallet.WalletConfig

@IntDef(
    SSOApplyTokenHandler.None,
    SSOApplyTokenHandler.Initial,
    SSOApplyTokenHandler.ReadyRegister,
    SSOApplyTokenHandler.Registered,
    SSOApplyTokenHandler.ReadyApproval,
    SSOApplyTokenHandler.Approval,
    SSOApplyTokenHandler.MintSuccess
)
annotation class SSOApplyTokenStatus

/**
 * 申请审批流程
 * 获取钱包已使用层数，派生铸币账户
 *
 * 通知服务器更新钱包层数 +1
 * 本地记录某一层铸币账户已经开始使用
 *
 * 给铸币账户打 10 个 vToken
 * 本地记录某一层铸币账户已经可以注册稳定币
 *
 * 铸币账户注册稳定币
 * 本地记录某一层铸币账户已经注册稳定币成功
 *
 * 主账户向 SSO 申请账户打 vToken
 * 本地记录某一层铸币账户已经准备审批，记录 SSO 申请账户的信息
 *
 * 通知服务器审批申请通过
 * 本地记录某一层铸币账户已经审批成功
 */
class SSOApplyTokenHandler(
    private val account: Account,
    private val mnemonics: List<String>,
    private var SSOApplyWalletAddress: String
) {
    companion object {
        /**
         * 没有任何准备
         */
        const val None = 0

        /**
         * 铸币账户已经准备好
         */
        const val Initial = 1

        /**
         * 铸币账户已经可以注册稳定币
         */
        const val ReadyRegister = 2

        /**
         * 铸币账户注册稳定币成功
         */
        const val Registered = 3

        /**
         * 已经可以审批 SSO 申请
         */
        const val ReadyApproval = 4

        /**
         * 审批 SSO 申请完成
         */
        const val Approval = 5

        /**
         * Mint 成功
         */
        const val MintSuccess = 6
    }


    private val mGovernorService by lazy {
        DataRepository.getGovernorService()
    }

    @WorkerThread
    suspend fun exec(): Boolean {
        val applyEngine = ApplyEngine()

        val findUnDoneRecord =
            applyEngine.getUnDoneRecord(account.getAddress().toHex(), SSOApplyWalletAddress)
        val layerWallet = if (findUnDoneRecord == null) {
            //todo net work error
            mGovernorService.getGovernorInfo(
                account.getAddress().toHex()
            ).data?.subAccountCount?.plus(
                1
            )
                ?: throw RuntimeException()
        } else {
            findUnDoneRecord.childNumber
        }

        val mintAccount = LibraWallet(WalletConfig(mnemonics)).generateAccount(layerWallet)

        applyEngine.addApplyHandle(
            SendWalletLayersHandle(
                layerWallet,
                account.getAddress().toHex()
            )
        )
        applyEngine.addApplyHandle(
            SendTokenAccountCoinHandle(
                account,
                layerWallet,
                mintAccount.getAddress().toHex(),
                10 * 1000000
            )
        )
        applyEngine.addApplyHandle(
            TokenAccountRegisterHandle(
                account.getAddress().toHex(),
                layerWallet,
                mintAccount,
                mintAccount.getAddress().toHex()
            )
        )
        applyEngine.addApplyHandle(
            SendSSOAccountCoinHandle(
                account.getAddress().toHex(),
                layerWallet,
                account,
                SSOApplyWalletAddress,
                100 * 1000000,
                mintAccount.getAddress().toHex()
            )
        )
        applyEngine.addApplyHandle(
            SendApplySSOHandle(
                account.getAddress().toHex(),
                layerWallet,
                mintAccount.getAddress().toHex(),
                SSOApplyWalletAddress
            )
        )

        return applyEngine.execSSOApply(findUnDoneRecord?.status)
    }
}