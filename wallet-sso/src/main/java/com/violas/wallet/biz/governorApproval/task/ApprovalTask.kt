package com.violas.wallet.biz.governorApproval.task

import com.violas.wallet.biz.TokenManager
import com.violas.wallet.repository.database.dao.SSOApplicationRecorDao
import com.violas.wallet.repository.http.governor.GovernorRepository

abstract class ApprovalTask {
    private var mServiceProvider: ServiceProvider? = null

    abstract suspend fun handle()

    fun setServiceProvider(serviceProvider: ServiceProvider) {
        mServiceProvider = serviceProvider
    }

    fun getServiceProvider() = mServiceProvider
}

interface ServiceProvider {
    fun getApplySsoRecordDao(): SSOApplicationRecorDao
    fun getGovernorService(): GovernorRepository
    fun getTokenManager():TokenManager
}