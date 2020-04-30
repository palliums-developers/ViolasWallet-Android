package com.violas.wallet.biz.governorApproval

import androidx.annotation.IntDef
import com.palliums.content.ContextProvider
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.governorApproval.task.ApprovalTask
import com.violas.wallet.biz.governorApproval.task.ServiceProvider
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.AppDatabase
import com.violas.wallet.repository.database.dao.SSOApplicationRecorDao
import com.violas.wallet.repository.database.entity.SSOApplicationRecordDo
import com.violas.wallet.repository.http.governor.GovernorRepository
import java.util.*

@IntDef(
    GovernorApprovalStatus.None,
    GovernorApprovalStatus.ReadyApproval,
    GovernorApprovalStatus.Approval,
    GovernorApprovalStatus.PublishSuccess,
    GovernorApprovalStatus.MintSuccess
)
annotation class GovernorApprovalStatus{
    companion object{
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
}

class ApprovalEngine {

    private val mApplyHandles = LinkedList<ApprovalTask>()

    private val mApplySsoRecordDao by lazy {
        AppDatabase.getInstance(ContextProvider.getContext()).ssoApplicationRecordDao()
    }

    private val mGovernorService by lazy {
        DataRepository.getGovernorService()
    }

    private val mTokenManager by lazy {
        TokenManager()
    }

    fun addApplyHandle(handle: ApprovalTask) {
        handle.setServiceProvider(object : ServiceProvider {
            override fun getApplySsoRecordDao(): SSOApplicationRecorDao {
                return mApplySsoRecordDao
            }

            override fun getGovernorService(): GovernorRepository {
                return mGovernorService
            }

            override fun getTokenManager(): TokenManager {
                return mTokenManager
            }
        })
        mApplyHandles.add(handle)
    }

    fun getUnApproveRecord(walletAddress: String, ssoWalletAddress: String): SSOApplicationRecordDo? {
        return mApplySsoRecordDao.findUnApproveRecord(walletAddress, ssoWalletAddress)
    }

    fun getUnMintRecord(walletAddress: String, ssoWalletAddress: String): SSOApplicationRecordDo? {
        return mApplySsoRecordDao.findUnMintRecord(walletAddress, ssoWalletAddress)
    }

    suspend fun execSSOApply(status: Int? = GovernorApprovalStatus.None) {
        val currentStatus = status ?: GovernorApprovalStatus.None
        for (index in GovernorApprovalStatus.None until currentStatus) {
            mApplyHandles.removeAt(0)
        }
        for (item in mApplyHandles) {
            item.handle()
        }
    }

    suspend fun execMint(status: Int? = GovernorApprovalStatus.Approval) {
        val currentStatus = status ?: GovernorApprovalStatus.Approval
        for (index in GovernorApprovalStatus.Approval until currentStatus) {
            mApplyHandles.removeAt(0)
        }
        for (item in mApplyHandles) {
            item.handle()
        }
    }
}