package com.violas.wallet.ui.main.bank

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.palliums.base.BaseViewModel
import com.palliums.utils.exceptionAsync
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.bank.AccountInfoDTO
import com.violas.wallet.repository.http.bank.BorrowingProductSummaryDTO
import com.violas.wallet.repository.http.bank.DepositProductSummaryDTO
import com.violas.wallet.utils.convertAmountToUSD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2020/8/21 17:05.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BankViewModel : BaseViewModel() {

    companion object {
        const val ACTION_LOAD_ACCOUNT_INFO = 0x01
        const val ACTION_LOAD_DEPOSIT_PRODUCTS = 0x02
        const val ACTION_LOAD_BORROWING_PRODUCTS = 0x04
    }

    // 银行账户信息
    private val accountInfoLiveData = MutableLiveData<AccountInfoDTO?>()

    // 显示金额
    val showAmountLiveData = MutableLiveData<Boolean>(true)

    // 存款总额
    val totalDepositLiveData = MediatorLiveData<String>()

    // 可借额度
    val borrowableLiveData = MediatorLiveData<String>()

    // 累计收益
    val totalEarningsLiveData = MediatorLiveData<String>()

    // 昨日收益
    val yesterdayEarningsLiveData = MediatorLiveData<String>()

    // 存款产品列表
    val depositProductsLiveData = MutableLiveData<List<DepositProductSummaryDTO>?>()

    // 借贷产品列表
    val borrowingProductsLiveData = MutableLiveData<List<BorrowingProductSummaryDTO>?>()

    private var address: String? = null
    private val bankService by lazy { DataRepository.getBankService() }

    init {
        val handleAmount: (Boolean, AccountInfoDTO?) -> Unit = { showAmount, accountInfo ->
            if (showAmount) {
                totalDepositLiveData.value =
                    "≈ ${convertAmountToUSD((accountInfo?.totalDeposit ?: 0.toDouble()) * 100)}"
                borrowableLiveData.value =
                    "≈ ${
                        convertAmountToUSD(
                            if (accountInfo != null && accountInfo.borrowableLimit > accountInfo.borrowed)
                                (accountInfo.borrowableLimit - accountInfo.borrowed) * 100
                            else
                                0.toDouble()
                        )
                    }/${
                        convertAmountToUSD((accountInfo?.borrowableLimit ?: 0.toDouble()) * 100)
                    }"
                totalEarningsLiveData.value =
                    "≈ ${convertAmountToUSD((accountInfo?.totalEarnings ?: 0.toDouble()) * 100)}"
                yesterdayEarningsLiveData.value =
                    "${convertAmountToUSD((accountInfo?.yesterdayEarnings ?: 0.toDouble()) * 100)} $"
            } else {
                totalDepositLiveData.value = "≈ ******"
                borrowableLiveData.value = "≈ ******"
                totalEarningsLiveData.value = "≈ ******"
                yesterdayEarningsLiveData.value = "***"
            }
        }
        totalDepositLiveData.addSource(showAmountLiveData) {
            handleAmount(it, accountInfoLiveData.value)
        }
        totalDepositLiveData.addSource(accountInfoLiveData) {
            handleAmount(showAmountLiveData.value == true, it)
        }
    }

    suspend fun initAddress() = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val violasAccount =
                AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
            val lastAddress = address
            address = violasAccount?.address

            // 如果是删除钱包后，立即重置用户银行的存款取款信息
            if (!lastAddress.isNullOrBlank()
                && address.isNullOrBlank()
                && accountInfoLiveData.value != null
            ) {
                accountInfoLiveData.postValue(null)
            }

            return@withContext !address.isNullOrBlank()
        }
    }

    fun toggleAmountShowHide() {
        showAmountLiveData.value = !(showAmountLiveData.value ?: true)
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        coroutineScope {
            val syncStartAddress: String?
            synchronized(lock) {
                syncStartAddress = this@BankViewModel.address
            }

            val accountInfoDeferred =
                if (syncStartAddress != null && action.and(ACTION_LOAD_ACCOUNT_INFO) != 0)
                    exceptionAsync { bankService.getAccountInfo(syncStartAddress) }
                else
                    null

            val depositProductsDeferred =
                if (action.and(ACTION_LOAD_DEPOSIT_PRODUCTS) != 0)
                    exceptionAsync { bankService.getDepositProducts() }
                else
                    null

            val borrowingProductsDeferred =
                if (action.and(ACTION_LOAD_BORROWING_PRODUCTS) != 0)
                    exceptionAsync { bankService.getBorrowingProducts() }
                else
                    null

            try {
                val depositProducts =
                    depositProductsDeferred?.await()
                depositProducts?.let {
                    depositProductsLiveData.postValue(it)
                }
            } catch (e: Exception) {
                depositProductsLiveData.postValue(null)
            }

            try {
                val borrowingProducts =
                    borrowingProductsDeferred?.await()
                borrowingProducts?.let {
                    borrowingProductsLiveData.postValue(it)
                }
            } catch (e: Exception) {
                borrowingProductsLiveData.postValue(null)
            }

            val accountInfo = accountInfoDeferred?.await()
            accountInfo?.let {
                val syncEndAddress: String?
                synchronized(lock) {
                    syncEndAddress = this@BankViewModel.address
                }

                if (syncStartAddress == syncEndAddress) {
                    accountInfoLiveData.postValue(accountInfo)
                }
            }
        }
    }
}