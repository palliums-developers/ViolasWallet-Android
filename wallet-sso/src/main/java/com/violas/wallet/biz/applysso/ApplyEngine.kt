package com.violas.wallet.biz.applysso

import com.palliums.content.ContextProvider
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.applysso.handler.ApplyHandle
import com.violas.wallet.biz.applysso.handler.ServiceProvider
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.AppDatabase
import com.violas.wallet.repository.database.dao.ApplySSORecordDao
import com.violas.wallet.repository.database.entity.ApplySSORecordDo
import com.violas.wallet.repository.http.governor.GovernorRepository
import java.util.*

class ApplyEngine {

    private val mApplyHandles = LinkedList<ApplyHandle>()

    private val mApplySsoRecordDao by lazy {
        AppDatabase.getInstance(ContextProvider.getContext()).applySsoRecordDao()
    }

    private val mGovernorService by lazy {
        DataRepository.getGovernorService()
    }

    private val mTokenManager by lazy {
        TokenManager()
    }

    fun addApplyHandle(handle: ApplyHandle) {
        handle.setServiceProvider(object : ServiceProvider {
            override fun getApplySsoRecordDao(): ApplySSORecordDao {
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

    fun getUnDoneRecord(walletAddress: String, ssoWalletAddress: String): ApplySSORecordDo? {
        return mApplySsoRecordDao.findSSOWalletUnDoneRecord(walletAddress, ssoWalletAddress)
            ?: return mApplySsoRecordDao.findUnDoneRecord(walletAddress)
    }

    fun getUnMintRecord(
        walletAddress: String,
        tokenIdx: Long,
        ssoWalletAddress: String
    ): ApplySSORecordDo? {
        return mApplySsoRecordDao.findUnMintRecord(
            walletAddress,
            tokenIdx,
            ssoWalletAddress
        )
    }

    suspend fun execSSOApply(status: Int? = SSOApplyTokenHandler.None) {
        val currentStatus = status ?: SSOApplyTokenHandler.None
        for (index in SSOApplyTokenHandler.None until currentStatus) {
            mApplyHandles.removeAt(0)
        }
        for (item in mApplyHandles) {
            item.handler()
        }
    }

    suspend fun execMint(status: Int? = SSOApplyTokenHandler.Approval) {
        val currentStatus = status ?: SSOApplyTokenHandler.Approval
        for (index in SSOApplyTokenHandler.Approval until currentStatus) {
            mApplyHandles.removeAt(0)
        }
        for (item in mApplyHandles) {
            item.handler()
        }
    }
}