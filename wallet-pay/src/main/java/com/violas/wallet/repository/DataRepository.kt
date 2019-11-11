package com.violas.wallet.repository

import com.violas.wallet.getContext
import com.violas.wallet.repository.database.AppDatabase
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import io.grpc.ManagedChannelBuilder
import org.palliums.libracore.admissioncontrol.LibraAdmissionControl

object DataRepository {
    private val appDatabase by lazy {
        AppDatabase.getInstance(getContext())
    }

    private val mChannel by lazy {
        ManagedChannelBuilder.forAddress("ac.testnet.libra.org", 8000)
            .usePlaintext()
            .build()
    }

    private val mViolasChannel by lazy {
        ManagedChannelBuilder.forAddress("47.106.208.207", 4000)
            .usePlaintext()
            .build()
    }

    fun getAccountStorage() = appDatabase.accountDao()

    fun getTokenStorage() = appDatabase.tokenDao()

    fun getAddressBookStorage() = appDatabase.addressBookDao()

    fun getBitcoinService() = BitcoinChainApi.get()

    fun getLibraService() = LibraAdmissionControl(mChannel)

    fun getViolasService() = LibraAdmissionControl(mViolasChannel)
}