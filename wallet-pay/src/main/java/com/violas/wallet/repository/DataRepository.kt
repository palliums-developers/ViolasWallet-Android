package com.violas.wallet.repository

import com.palliums.content.ContextProvider.getContext
import com.palliums.violas.http.ViolasApi
import com.palliums.violas.http.ViolasRepository
import com.palliums.violas.http.ViolasService
import com.palliums.violas.smartcontract.multitoken.MultiContractRpcApi
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.BuildConfig
import com.violas.wallet.common.BaseBizUrl.getLibraBaseUrl
import com.violas.wallet.common.BaseBizUrl.getViolasBaseUrl
import com.violas.wallet.common.BaseBizUrl.getViolasChainUrl
import com.violas.wallet.repository.database.AppDatabase
import com.violas.wallet.repository.http.bank.BankApi
import com.violas.wallet.repository.http.bank.BankRepository
import com.violas.wallet.repository.http.basic.BasicApi
import com.violas.wallet.repository.http.basic.BasicRepository
import com.violas.wallet.repository.http.bitcoin.trezor.BitcoinTrezorApi
import com.violas.wallet.repository.http.bitcoin.trezor.BitcoinTrezorRepository
import com.violas.wallet.repository.http.bitcoin.trezor.BitcoinTrezorService
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import com.violas.wallet.repository.http.exchange.ExchangeApi
import com.violas.wallet.repository.http.exchange.ExchangeRepository
import com.violas.wallet.repository.http.incentive.IncentiveApi
import com.violas.wallet.repository.http.incentive.IncentiveRepository
import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor
import com.violas.wallet.repository.http.interceptor.RequestHeaderInterceptor
import com.violas.wallet.repository.http.libra.violas.LibraViolasApi
import com.violas.wallet.repository.http.libra.violas.LibraViolasRepository
import com.violas.wallet.repository.http.libra.violas.LibraViolasService
import com.violas.wallet.repository.http.mapping.MappingApi
import com.violas.wallet.repository.http.mapping.MappingRepository
import com.violas.wallet.repository.http.message.MessageApi
import com.violas.wallet.repository.http.message.MessageRepository
import com.violas.wallet.repository.http.violas.ViolasBizService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.palliums.libracore.http.LibraRpcRepository
import org.palliums.libracore.http.LibraRpcService
import org.palliums.violascore.http.ViolasRpcRepository
import org.palliums.violascore.http.ViolasRpcService
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
            .callTimeout(100, TimeUnit.SECONDS)
            .connectTimeout(100, TimeUnit.SECONDS)
            .readTimeout(100, TimeUnit.SECONDS)
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

    fun getLibraRpcService() =
        LibraRpcService(LibraRpcRepository(okHttpClient, getLibraBaseUrl()))

    fun getLibraBizService() =
        LibraViolasService(LibraViolasRepository(retrofit.create(LibraViolasApi::class.java)))

    fun getViolasBizService() =
        ViolasBizService(ViolasRepository(retrofit.create(ViolasApi::class.java)))

    fun getViolasService() =
        ViolasService(ViolasRepository(retrofit.create(ViolasApi::class.java)))

    fun getViolasChainRpcService() =
        ViolasRpcService(ViolasRpcRepository(okHttpClient, getViolasChainUrl()))

    fun getTransactionRecordService(coinTypes: CoinTypes) =
        when (coinTypes) {
            CoinTypes.Violas -> {
                getViolasBizService()
            }

            CoinTypes.Libra -> {
                LibraViolasService(
                    LibraViolasRepository(retrofit.create(LibraViolasApi::class.java))
                )
            }

            else -> {
                BitcoinTrezorService(
                    BitcoinTrezorRepository(retrofit.create(BitcoinTrezorApi::class.java))
                )
            }
        }

    fun getMappingService() =
        MappingRepository(retrofit.create(MappingApi::class.java))

    fun getExchangeService() =
        ExchangeRepository(retrofit.create(ExchangeApi::class.java))

    fun getBankService() =
        BankRepository(retrofit.create(BankApi::class.java))

    fun getBasicService() =
        BasicRepository(retrofit.create(BasicApi::class.java))

    fun getIncentiveService() =
        IncentiveRepository(retrofit.create(IncentiveApi::class.java))

    fun getMessageService() =
        MessageRepository(retrofit.create(MessageApi::class.java))

    fun getMultiTokenContractService(): MultiContractRpcApi {
        return retrofit.create(MultiContractRpcApi::class.java)
    }
}