package com.violas.wallet.biz.applysso

import androidx.annotation.IntDef
import com.palliums.content.ContextProvider
import com.violas.wallet.biz.applysso.handler.*
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.AppDatabase
import org.palliums.violascore.wallet.Account
import org.palliums.violascore.wallet.LibraWallet
import org.palliums.violascore.wallet.WalletConfig
import java.lang.RuntimeException
import java.util.*

@IntDef(
    SSOApplyTokenHandler.None,
    SSOApplyTokenHandler.Initial,
    SSOApplyTokenHandler.ReadyRegister,
    SSOApplyTokenHandler.Registered,
    SSOApplyTokenHandler.ReadyApproval,
    SSOApplyTokenHandler.Approval
)
annotation class SSOApplyTokenStatus

/**
 * 申请审批流程
 * 获取钱包已使用层数，派生铸币账户
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
    private val accountId: Long,
    private val account: Account,
    private val mnemonics: List<String>,
    private val walletAddress: String
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
    }

    private val mApplySsoRecordDao by lazy {
        AppDatabase.getInstance(ContextProvider.getContext()).applySsoRecordDao()
    }

    private val mGovernorService by lazy {
        DataRepository.getGovernorService()
    }

    suspend fun exec(): Boolean {
        val findUnDoneRecord = mApplySsoRecordDao.findUnDoneRecord(accountId)
        val layerWallet = if (findUnDoneRecord == null) {
            //todo net work error
            mGovernorService.getGovernorInfo(walletAddress).data?.subAccountCount?.plus(1)
                ?: throw RuntimeException()
        } else {
            findUnDoneRecord.childNumber
        }

        val libraWallet = LibraWallet(WalletConfig(mnemonics))
        val tokenAccount = libraWallet.generateAccount(layerWallet)

        val linkedList = LinkedList<ApplyHandle>()
        linkedList.add(SendWalletLayersHandle(layerWallet, accountId, walletAddress))
        linkedList.add(SendTokenAccountCoinHandle(account, tokenAccount.getAddress().toBytes(), 10))
        linkedList.add(
            TokenAccountRegisterHandle(
                tokenAccount,
                tokenAccount.getAddress().toBytes()
            )
        )
        linkedList.add(
            SendSSOAccountCoinHandle(
                tokenAccount,
                tokenAccount.getAddress().toBytes(),
                1000
            )
        )
        linkedList.add(SendWalletLayersHandle(layerWallet, accountId, walletAddress))

        return ApplyEngine(linkedList).execSSOApply(findUnDoneRecord?.status)
    }
}