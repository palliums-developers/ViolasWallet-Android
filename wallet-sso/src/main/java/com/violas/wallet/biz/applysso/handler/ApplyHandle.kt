package com.violas.wallet.biz.applysso.handler

import com.violas.wallet.repository.database.dao.ApplySSORecordDao
import com.violas.wallet.repository.http.governor.GovernorRepository

abstract class ApplyHandle {
    private var mServiceProvider: ServiceProvider? = null

    abstract fun handler(): Boolean

    fun setServiceProvider(serviceProvider: ServiceProvider) {
        mServiceProvider = serviceProvider
    }

    fun getServiceProvider() = mServiceProvider
}

interface ServiceProvider {
    fun getApplySsoRecordDao(): ApplySSORecordDao
    fun getGovernorService(): GovernorRepository
}