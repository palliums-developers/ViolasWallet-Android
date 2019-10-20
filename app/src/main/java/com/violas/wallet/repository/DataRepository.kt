package com.violas.wallet.repository

import com.violas.wallet.getContext
import com.violas.wallet.repository.database.AppDatabase

object DataRepository {
    private val appDatabase by lazy {
        AppDatabase.getInstance(getContext())
    }

    fun getAccountStorage() = appDatabase.accountDao()

    fun getAddressBookStorage() = appDatabase.addressBookDao()
}