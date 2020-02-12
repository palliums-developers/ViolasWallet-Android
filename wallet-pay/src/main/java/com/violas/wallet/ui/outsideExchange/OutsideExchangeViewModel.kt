package com.violas.wallet.ui.outsideExchange

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palliums.content.ContextProvider
import com.palliums.utils.coroutineExceptionHandler
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ExchangeMappingManager
import com.violas.wallet.biz.ExchangePair
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.repository.database.entity.AccountDO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class OutsideExchangeViewModel : ViewModel() {
    private lateinit var mAccount: AccountDO
    private val mAccountManager = AccountManager()

    private val mCurrentExchangePairLiveData = MutableLiveData<ExchangePair>()

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

    private val mExchangePairManager by lazy {
        mExchangeMappingManager.getExchangePair()
    }

    @MainThread
    fun exchange(accountId: Long) {
        init()
        viewModelScope.launch(Dispatchers.IO) {
            mAccount = mAccountManager.getAccountById(accountId)
            val exchangePair = mExchangePairManager.findExchangePair(mAccount.coinNumber)
                ?: return@launch
            mCurrentExchangePairLiveData.postValue(exchangePair)
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
    private fun observerExchangeCoinType() = mCurrentExchangePairLiveData.observeForever {
        exchangeFromCoinLiveData.value = it.getFirst().getName()
        exchangeToCoinLiveData.value = it.getLast().getName()
        exchangeRateLiveData.value = it.getRate()
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
            try {
                val receivingAccount =
                    stableCurrencyReceivingAccountLiveData.value ?: throw RuntimeException()
                val exchangePair =
                    mCurrentExchangePairLiveData.value ?: throw java.lang.RuntimeException()

                mExchangeMappingManager.exchangeMapping(
                    mExchangeMappingManager.parseFirstMappingAccount(
                        exchangePair,
                        mAccount,
                        accountSendPrivate
                    ),
                    mExchangeMappingManager.parseLastMappingAccount(
                        exchangePair,
                        receivingAccount,
                        accountReceivePrivate
                    ),
                    mToCoinAmountLiveData.value ?: BigDecimal("0.0001"),
                    exchangePair.getReceiveFirstAddress()
                )
                success.invoke()
            } catch (e: Exception) {
                error.invoke(e)
            }
        }
    }

    fun getExchangePairChainName(): Pair<String, String> {
        val value = mCurrentExchangePairLiveData.value!!
        return Pair(
            value.getFirst().getCoinType().fullName(),
            value.getLast().getCoinType().fullName()
        )
    }

    fun switchReceiveAccount(accountId: Long) {
        if (accountId == stableCurrencyReceivingAccountLiveData.value?.id) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val account = mAccountManager.getAccountById(accountId)
                if (account.coinNumber == CoinTypes.Violas.coinType()) {
                    stableCurrencyReceivingAccountLiveData.postValue(account)
                }
            } catch (e: Exception) {
            }
        }
    }
}
