package com.violas.wallet.biz.applysso.handler

import com.violas.wallet.biz.TokenManager
import com.violas.wallet.repository.database.dao.ApplySSORecordDao
import com.violas.wallet.repository.http.governor.GovernorRepository

abstract class ApplyHandle {
    private var mServiceProvider: ServiceProvider? = null

    abstract suspend fun handler()

    fun setServiceProvider(serviceProvider: ServiceProvider) {
        mServiceProvider = serviceProvider
    }

    fun getServiceProvider() = mServiceProvider
}

interface ServiceProvider {
    fun getApplySsoRecordDao(): ApplySSORecordDao
    fun getGovernorService(): GovernorRepository
    fun getTokenManager():TokenManager
}