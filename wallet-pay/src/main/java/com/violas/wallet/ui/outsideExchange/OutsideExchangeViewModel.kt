package com.violas.wallet.ui.outsideExchange

import android.app.Application
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.content.ContextProvider
import com.palliums.utils.coroutineExceptionHandler
import com.palliums.utils.getString
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.*
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.wallet.KeyPair
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.CountDownLatch

class OutsideExchangeViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var mAccount: AccountDO
    private val mAccountManager = AccountManager()
    private val mTokenManager = TokenManager()
    private val mViolasService = DataRepository.getViolasService()

    private val exchangeCoinTypeLiveData = MutableLiveData<Int>()

    val exchangeFromCoinLiveData = MutableLiveData<String>()
    val exchangeToCoinLiveData = MutableLiveData<String>()

    private val exchangeRateLiveData = MutableLiveData<BigDecimal>(BigDecimal.valueOf(0))
    val exchangeRateValueLiveData = MutableLiveData<String>("... = ...")
    /**
     * 兑换的稳定币接收地址
     */
    val stableCurrencyReceivingAccountLiveData = MutableLiveData<AccountDO>()
    // 兑换数量
    val mFromCoinAmountLiveData = MutableLiveData<BigDecimal>()
    val mToCoinAmountLiveData = MutableLiveData<BigDecimal>()

    private val mExchangeMappingManager by lazy {
        ExchangeMappingManager()
    }

    @MainThread
    fun exchange(accountId: Long) {
        init()
        viewModelScope.launch(Dispatchers.IO) {
            mAccount = mAccountManager.getAccountById(accountId)

            when (mAccount.coinNumber) {
                CoinTypes.Bitcoin.coinType(),
                CoinTypes.BitcoinTest.coinType(),
                CoinTypes.Libra.coinType() -> {
                    withContext(Dispatchers.Main) {
                        exchangeCoinTypeLiveData.postValue(mAccount.coinNumber)
                    }
                }
                else -> {
                    TODO()
                }
            }
        }
    }

    private fun init() {
        observerExchangeRate()
        observerExchangeCoinType()
        viewModelScope.launch(Dispatchers.IO) {
            stableCurrencyReceivingAccountLiveData.postValue(
                mAccountManager.getIdentityByCoinType(CoinTypes.Violas.coinType())
            )
        }
    }

    private fun observerExchangeRate() = exchangeRateLiveData.observeForever {
        exchangeRateValueLiveData.value =
            "1 ${exchangeFromCoinLiveData.value ?: ""} = ${it.setScale(
                4,
                RoundingMode.DOWN
            ).stripTrailingZeros().toPlainString()} ${exchangeToCoinLiveData.value ?: ""}"
    }

    /**
     * 处理处理交易币种切换的时候
     */
    private fun observerExchangeCoinType() = exchangeCoinTypeLiveData.observeForever {
        when (it) {
            CoinTypes.BitcoinTest.coinType(),
            CoinTypes.Bitcoin.coinType() -> {
                exchangeFromCoinLiveData.value = ("BTC")
                exchangeToCoinLiveData.value = ("vBTC")
            }
            CoinTypes.Libra.coinType() -> {
                exchangeFromCoinLiveData.value = ("Libra")
                exchangeToCoinLiveData.value = ("vLibra")
            }
            else -> {
                TODO("待处理")
            }
        }

        exchangeRateLiveData.value = (BigDecimal.valueOf(1))
    }

    fun changeToCoinAmount(get: String?) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler()) {
            val amount =
                if (exchangeRateValueLiveData.value != null && get != null && get.isNotEmpty()) {
                    BigDecimal(get)
                        .multiply(exchangeRateLiveData.value!!)
                        .setScale(8, RoundingMode.DOWN)
                } else {
                    BigDecimal("0")
                }
            mToCoinAmountLiveData.postValue(amount)
        }
    }

    fun changeFromCoinAmount(get: String?) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler()) {
            val amount =
                if (exchangeRateValueLiveData.value != null && get != null && get.isNotEmpty()) {
                    BigDecimal(get)
                        .divide(
                            exchangeRateLiveData.value!!,
                            8,
                            RoundingMode.UP
                        )
                } else {
                    BigDecimal("0")
                }
            mFromCoinAmountLiveData.postValue(amount)
        }
    }

    fun decryptSendAccountKey(password: String): ByteArray? {
        return SimpleSecurity.instance(ContextProvider.getContext()).decrypt(
            password.toByteArray(),
            mAccount.privateKey
        )
    }

    fun decryptReceiveAccountKey(password: String): ByteArray? {
        return SimpleSecurity.instance(ContextProvider.getContext()).decrypt(
            password.toByteArray(),
            stableCurrencyReceivingAccountLiveData.value?.privateKey
        )
    }

    fun initiateChange(
        accountSendPrivate: ByteArray,
        accountReceivePrivate: ByteArray,
        success: () -> Unit,
        error: (Throwable) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler()) {
            val receivingAccount =
                stableCurrencyReceivingAccountLiveData.value ?: throw RuntimeException()

            val checkTokenRegister = mViolasService.checkTokenRegister(
                receivingAccount.address,
                mExchangeMappingManager.currentTokenAddress()
            )

            if (!checkTokenRegister) {
                val publishToken = publishToken(
                    Account(KeyPair(accountReceivePrivate)),
                    mExchangeMappingManager.currentTokenAddress()
                )
                if (!publishToken) {
                    error(RuntimeException(getString(R.string.hint_exchange_error)))
                    return@launch
                }
            }

            mExchangeMappingManager.exchangeBTC2vBTC(
                SendAccount(
                    mAccount.address,
                    accountSendPrivate,
                    mAccount.publicKey.hexToBytes()
                ),
                mToCoinAmountLiveData.value?.toDouble() ?: 0.001,
                "2MxBZG7295wfsXaUj69quf8vucFzwG35UWh",
                ReceiveTokenAccount(
                    receivingAccount.address,
                    mExchangeMappingManager.currentTokenAddress()
                )
                , {
                    Log.e("===", it)
                }, {
                    Log.e("===", it.message)
                }
            )
        }
    }

    private fun publishToken(mAccount: Account, tokenAddress: String): Boolean {
        val countDownLatch = CountDownLatch(1)
        var exec = false
        DataRepository.getViolasService()
            .publishToken(
                getApplication(),
                mAccount,
                tokenAddress
            ) {
                exec = it
                countDownLatch.countDown()
            }
        try {
            countDownLatch.await()
        } catch (e: Exception) {
            exec = false
        }
        return exec
    }
}
