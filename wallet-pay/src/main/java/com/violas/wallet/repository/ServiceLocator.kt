package com.violas.wallet.repository

import com.palliums.content.ContextProvider.getContext
import com.palliums.violas.http.ViolasApi
import com.palliums.violas.http.ViolasService
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.BuildConfig
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.database.AppDatabase
import com.violas.wallet.repository.http.TransactionRepository
import com.violas.wallet.repository.http.bitcoin.BitmainApi
import com.violas.wallet.repository.http.bitcoin.BitmainRepository
import com.violas.wallet.repository.http.bitcoin.BitmainService
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor
import com.violas.wallet.repository.http.libra.LibexplorerApi
import com.violas.wallet.repository.http.libra.LibexplorerRepository
import com.violas.wallet.repository.http.libra.LibexplorerService
import com.violas.wallet.repository.http.violas.ViolasRepository
import io.grpc.ManagedChannelBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.palliums.libracore.admissioncontrol.LibraAdmissionControl
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ServiceLocator {
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

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor())
            .addInterceptor(HttpLoggingInterceptor().also {
                it.level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            .callTimeout(40, TimeUnit.SECONDS)
            .connectTimeout(40, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(getDefaultBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
    }

    fun getDefaultBaseUrl(): String {
        return if (Vm.TestNet) ViolasApi.BASE_URL_TEST_NET else ViolasApi.BASE_URL_MAIN_NET
    }

    fun getAccountStorage() = appDatabase.accountDao()

    fun getTokenStorage() = appDatabase.tokenDao()

    fun getAddressBookStorage() = appDatabase.addressBookDao()

    fun getBitcoinService() = BitcoinChainApi.get()

    fun getLibraService() = LibraAdmissionControl(mChannel)

    fun getViolasRepository(): ViolasRepository {
        return ViolasRepository(ViolasService(retrofit.create(ViolasApi::class.java)))
    }

    fun getLibexplorerRepository(): LibexplorerRepository {
        return LibexplorerRepository(LibexplorerService(retrofit.create(LibexplorerApi::class.java)))
    }

    fun getBitmainRepository(): BitmainRepository {
        return BitmainRepository(BitmainService(retrofit.create(BitmainApi::class.java)))
    }

    fun getTransactionRepository(coinTypes: CoinTypes): TransactionRepository {
        return when (coinTypes) {
            CoinTypes.VToken -> {
                getViolasRepository()
            }

            CoinTypes.Libra -> {
                getLibexplorerRepository()
            }

            else -> {
                getBitmainRepository()
            }
        }
    }
}