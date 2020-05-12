package com.violas.wallet.biz.governorApproval

import androidx.annotation.IntDef
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.governorApproval.task.ApprovalTask
import com.violas.wallet.biz.governorApproval.task.ServiceProvider
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.dao.SSOApplicationRecorDao
import com.violas.wallet.repository.database.entity.SSOApplicationRecordDo
import com.violas.wallet.repository.http.governor.GovernorRepository
import java.util.*

@IntDef(
    GovernorApprovalStatus.NONE,
    GovernorApprovalStatus.TRANSFERRED,
    GovernorApprovalStatus.NOTIFIED,
    GovernorApprovalStatus.PUBLISHED,
    GovernorApprovalStatus.MINTED
)
annotation class GovernorApprovalStatus {
    companion object {
        /**
         * 没有任何准备
         */
        const val NONE = 0

        /**
         * 已转平台币给发行商
         */
        const val TRANSFERRED = 1

        /**
         * 已通知发行商可以申请铸币
         */
        const val NOTIFIED = 2

        /**
         * 已 publish 合约
         */
        const val PUBLISHED = 3

        /**
         * 已铸稳定币给发行商
         */
        const val MINTED = 4
    }
}

class ApprovalEngine {

    private val mApprovalTasks = LinkedList<ApprovalTask>()

    private val mSSOApplicationRecordStorage by lazy {
        DataRepository.getSSOApplicationRecordStorage()
    }

    private val mGovernorService by lazy {
        DataRepository.getGovernorService()
    }

    private val mTokenManager by lazy {
        TokenManager()
    }

    fun addApprovalTask(task: ApprovalTask) {
        task.setServiceProvider(object : ServiceProvider {
            override fun getSSOApplicationRecordStorage(): SSOApplicationRecorDao {
                return mSSOApplicationRecordStorage
            }

            override fun getGovernorService(): GovernorRepository {
                return mGovernorService
            }

            override fun getTokenManager(): TokenManager {
                return mTokenManager
            }
        })
        mApprovalTasks.add(task)
    }

    fun getUnTransferRecord(
        walletAddress: String,
        ssoWalletAddress: String
    ): SSOApplicationRecordDo? {
        return mSSOApplicationRecordStorage.findUnApproveRecord(walletAddress, ssoWalletAddress)
    }

    fun getUnMintRecord(walletAddress: String, ssoWalletAddress: String): SSOApplicationRecordDo? {
        return mSSOApplicationRecordStorage.findUnMintRecord(walletAddress, ssoWalletAddress)
    }

    suspend fun execTransfer(status: Int? = GovernorApprovalStatus.NONE) {
        val currentStatus = status ?: GovernorApprovalStatus.NONE
        for (index in GovernorApprovalStatus.NONE until currentStatus) {
            mApprovalTasks.removeAt(0)
        }
        for (item in mApprovalTasks) {
            item.handle()
        }
    }

    suspend fun execMint(status: Int? = GovernorApprovalStatus.NOTIFIED) {
        val currentStatus = status ?: GovernorApprovalStatus.NOTIFIED
        for (index in GovernorApprovalStatus.NOTIFIED until currentStatus) {
            mApprovalTasks.removeAt(0)
        }
        for (item in mApprovalTasks) {
            item.handle()
        }
    }
}