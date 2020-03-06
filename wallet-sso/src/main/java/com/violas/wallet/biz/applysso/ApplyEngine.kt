package com.violas.wallet.biz.applysso

import android.util.Log
import com.palliums.content.ContextProvider
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

    fun execSSOApply(status: Int? = SSOApplyTokenHandler.None): Boolean {
        val currentStatus = status ?: SSOApplyTokenHandler.None
        var isSuccess = false
        for (index in SSOApplyTokenHandler.None until currentStatus) {
            mApplyHandles.removeAt(0)
        }
        for (item in mApplyHandles) {
            val handler = item.handler()
            if (!handler) {
                isSuccess = false
                break
            }
        }
        return isSuccess
    }

    fun addApplyHandle(handle: ApplyHandle) {
        handle.setServiceProvider(object : ServiceProvider {
            override fun getApplySsoRecordDao(): ApplySSORecordDao {
                return mApplySsoRecordDao
            }

            override fun getGovernorService(): GovernorRepository {
                return mGovernorService
            }
        })
        mApplyHandles.add(handle)
    }

    fun getUnDoneRecord(walletAddress: String, SSOApplyWalletAddress: String): ApplySSORecordDo? {
        return mApplySsoRecordDao.findSSOWalletUnDoneRecord(walletAddress, SSOApplyWalletAddress)
            ?: return mApplySsoRecordDao.findUnDoneRecord(walletAddress)
    }

    fun getUnMintRecord(mintTokenAddress: String): ApplySSORecordDo? {
        return mApplySsoRecordDao.findUnMintRecord(mintTokenAddress)
    }

    fun execMint(status: Int? = SSOApplyTokenHandler.Approval): Boolean {
        val currentStatus = status ?: SSOApplyTokenHandler.Approval
        var isSuccess = false
        for (index in SSOApplyTokenHandler.None until currentStatus) {
            mApplyHandles.removeAt(0)
        }
        for (item in mApplyHandles) {
            val handler = item.handler()
            if (!handler) {
                isSuccess = false
                Log.e("ApplyEngine", "ApplyEngine error ${item.javaClass.simpleName}")
                break
            }
        }
        return isSuccess
    }
}