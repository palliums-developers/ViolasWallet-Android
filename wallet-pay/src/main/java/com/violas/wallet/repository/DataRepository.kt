package com.violas.wallet.repository

import com.smallraw.core.http.violas.ViolasApi
import com.smallraw.core.http.violas.ViolasRepository
import com.violas.wallet.BuildConfig
import com.violas.wallet.common.Vm
import com.violas.wallet.getContext
import com.violas.wallet.repository.database.AppDatabase
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor
import com.violas.wallet.repository.http.violas.ViolasService
import io.grpc.ManagedChannelBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.palliums.libracore.admissioncontrol.LibraAdmissionControl
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

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

    private const val VIOLAS_URL_MAIN_NET = "http://52.27.228.84:4000/"
    private const val VIOLAS_URL_TEST_NET = "http://52.27.228.84:4000/"

    private val violasOkHttp by lazy {
        OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor())
            .addInterceptor(HttpLoggingInterceptor().also {
                it.level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.HEADERS
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            .callTimeout(40, TimeUnit.SECONDS)
            .connectTimeout(40, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .build()
    }

    private val violasRetrofit by lazy {
        Retrofit.Builder()
            .client(violasOkHttp)
            .baseUrl(if (Vm.TestNet) VIOLAS_URL_TEST_NET else VIOLAS_URL_MAIN_NET)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
    }

    fun getAccountStorage() = appDatabase.accountDao()

    fun getTokenStorage() = appDatabase.tokenDao()

    fun getAddressBookStorage() = appDatabase.addressBookDao()

    fun getBitcoinService() = BitcoinChainApi.get()

    fun getLibraService() = LibraAdmissionControl(mChannel)

    fun getViolasService() =
        ViolasService(ViolasRepository(violasRetrofit.create(ViolasApi::class.java)))
}