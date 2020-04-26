package com.violas.wallet.repository

import com.palliums.content.ContextProvider.getContext
import com.palliums.violas.http.ViolasApi
import com.palliums.violas.http.ViolasRepository
import com.palliums.violas.http.ViolasService
import com.palliums.violas.smartcontract.multitoken.MultiContractRpcApi
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.BuildConfig
import com.violas.wallet.common.BaseBizUrl.getViolasBaseUrl
import com.violas.wallet.repository.database.AppDatabase
import com.violas.wallet.repository.http.bitcoin.BitmainApi
import com.violas.wallet.repository.http.bitcoin.BitmainRepository
import com.violas.wallet.repository.http.bitcoin.BitmainService
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import com.violas.wallet.repository.http.dex.DexApi
import com.violas.wallet.repository.http.dex.DexRepository
import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor
import com.violas.wallet.repository.http.interceptor.RequestHeaderInterceptor
import com.violas.wallet.repository.http.libra.violas.LibraViolasApi
import com.violas.wallet.repository.http.libra.violas.LibraViolasRepository
import com.violas.wallet.repository.http.libra.violas.LibraViolasService
import com.violas.wallet.repository.http.mappingExchange.MappingExchangeApi
import com.violas.wallet.repository.http.mappingExchange.MappingExchangeRepository
import com.violas.wallet.repository.http.violas.ViolasBizService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.palliums.libracore.http.LibraApi
import org.palliums.libracore.http.LibraRepository
import org.palliums.libracore.http.LibraService
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object DataRepository {
    private val appDatabase by lazy {
        AppDatabase.getInstance(getContext())
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor())
            .addInterceptor(RequestHeaderInterceptor())
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
            .baseUrl(getViolasBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
    }

    fun getAccountStorage() = appDatabase.accountDao()

    fun getTokenStorage() = appDatabase.tokenDao()

    fun getAddressBookStorage() = appDatabase.addressBookDao()

    fun getBitcoinService() = BitcoinChainApi.get()

    fun getLibraService() =
        LibraService(LibraRepository(retrofit.create(LibraApi::class.java)))

    fun getLibraBizService() =
        LibraViolasService(LibraViolasRepository(retrofit.create(LibraViolasApi::class.java)))

    fun getViolasBizService() =
        ViolasBizService(ViolasRepository(retrofit.create(ViolasApi::class.java)))

    fun getViolasService() =
        ViolasService(ViolasRepository(retrofit.create(ViolasApi::class.java)))

    fun getTransactionService(coinTypes: CoinTypes) =
        when (coinTypes) {
            CoinTypes.Violas -> {
                getViolasBizService()
            }

            CoinTypes.Libra -> {
                LibraViolasService(LibraViolasRepository(retrofit.create(LibraViolasApi::class.java)))
            }

            else -> {
                BitmainService(BitmainRepository(retrofit.create(BitmainApi::class.java)))
            }
        }

    fun getDexService() =
        DexRepository(retrofit.create(DexApi::class.java))

    fun getMappingExchangeService() =
        MappingExchangeRepository(retrofit.create(MappingExchangeApi::class.java))

    fun getMultiTokenContractService(): MultiContractRpcApi {
        return retrofit.create(MultiContractRpcApi::class.java)
    }
}