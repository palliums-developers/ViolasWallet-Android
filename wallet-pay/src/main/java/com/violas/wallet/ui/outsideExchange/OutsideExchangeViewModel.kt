package com.violas.wallet.ui.outsideExchange

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palliums.content.ContextProvider
import com.palliums.utils.coroutineExceptionHandler
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ExchangeMappingManager
import com.violas.wallet.biz.ReceiveTokenAccount
import com.violas.wallet.biz.SendAccount
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.repository.database.entity.AccountDO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.palliums.libracore.serialization.hexToBytes
import java.math.BigDecimal
import java.math.RoundingMode

class OutsideExchangeViewModel : ViewModel() {
    private lateinit var mAccount: AccountDO
    private val mAccountManager = AccountManager()

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

    fun initiateChange() {
        viewModelScope.launch {
            mExchangeMappingManager.exchangeBTC2vBTC(
                SendAccount(
                    mAccount.address,
                    SimpleSecurity.instance(ContextProvider.getContext()).decrypt(
                        "Asd123123".toByteArray(),
                        mAccount.privateKey
                    )!!,
                    mAccount.publicKey.hexToBytes()
                ),
                mToCoinAmountLiveData.value?.toDouble() ?: 0.001,
                "2MxBZG7295wfsXaUj69quf8vucFzwG35UWh",
                ReceiveTokenAccount(
                    stableCurrencyReceivingAccountLiveData.value!!.address,
                    "af955c1d62a74a7543235dbb7fa46ed98948d2041dff67dfdb636a54e84f91fb"
                )
                , {
                    Log.e("===", it)
                }, {
                    Log.e("===", it.message)
                }
            )
        }
    }
}
