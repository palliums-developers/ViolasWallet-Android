package com.violas.wallet.ui.main.bank

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.palliums.base.BaseViewModel
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.bank.BorrowingProductSummaryDTO
import com.violas.wallet.repository.http.bank.DepositProductSummaryDTO
import com.violas.wallet.repository.http.bank.UserBankInfoDTO
import com.violas.wallet.utils.keepTwoDecimals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2020/8/21 17:05.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BankViewModel : BaseViewModel() {

    // 显示金额
    val showAmountLiveData = MutableLiveData<Boolean>(true)

    // 存款总额
    val totalDepositLiveData = MediatorLiveData<String>()

    // 可借总额
    val totalBorrowableLiveData = MediatorLiveData<String>()

    // 累计收益
    val totalEarningsLiveData = MediatorLiveData<String>()

    // 昨日收益
    val yesterdayEarningsLiveData = MediatorLiveData<String>()

    // 用户银行信息
    val userBankInfoLiveData = MutableLiveData<UserBankInfoDTO>()

    private var address: String? = null
    private val bankService by lazy { DataRepository.getBankService() }

    init {
        val hiddenAmount: () -> Unit = {
            totalDepositLiveData.value = "≈ ******"
            totalBorrowableLiveData.value = "≈ ******"
            totalEarningsLiveData.value = "≈ ******"
            yesterdayEarningsLiveData.value = "***"
        }
        totalDepositLiveData.addSource(showAmountLiveData) {
            if (it) {
                val userBankInfo = userBankInfoLiveData.value
                totalDepositLiveData.value =
                    "≈ ${keepTwoDecimals(userBankInfo?.totalDeposit ?: "0")}"
                totalBorrowableLiveData.value =
                    "≈ ${keepTwoDecimals(userBankInfo?.totalBorrowable ?: "0")}"
                totalEarningsLiveData.value =
                    "≈ ${keepTwoDecimals(userBankInfo?.totalEarnings ?: "0")}"
                yesterdayEarningsLiveData.value =
                    "${keepTwoDecimals(userBankInfo?.yesterdayEarnings ?: "0")} $"
            } else {
                hiddenAmount()
            }
        }
        totalDepositLiveData.addSource(userBankInfoLiveData) {
            if (showAmountLiveData.value == true) {
                totalDepositLiveData.value = "≈ ${keepTwoDecimals(it.totalDeposit)}"
                totalBorrowableLiveData.value = "≈ ${keepTwoDecimals(it.totalBorrowable)}"
                totalEarningsLiveData.value = "≈ ${keepTwoDecimals(it.totalEarnings)}"
                yesterdayEarningsLiveData.value = "${keepTwoDecimals(it.yesterdayEarnings)} $"
            } else {
                hiddenAmount()
            }
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
                && userBankInfoLiveData.value != null
            ) {
                userBankInfoLiveData.value = userBankInfoLiveData.value!!.also {
                    it.totalDeposit = "0"
                    it.totalBorrowable = "0"
                    it.totalEarnings = "0"
                    it.yesterdayEarnings = "0"
                }
            }

            return@withContext !address.isNullOrBlank()
        }
    }

    fun toggleAmountShowHide() {
        showAmountLiveData.value = !(showAmountLiveData.value ?: true)
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        val address: String
        synchronized(lock) {
            address = this.address ?: ""
        }
        val userBankInfo = bankService.getUserBankInfo(address)
        userBankInfoLiveData.postValue(userBankInfo)
    }

    private fun fakeData(address: String?): UserBankInfoDTO {
        return UserBankInfoDTO(
            totalDeposit = if (address.isNullOrBlank()) "0" else "1000.11",
            totalBorrowable = if (address.isNullOrBlank()) "0" else "500.22",
            totalEarnings = if (address.isNullOrBlank()) "0" else "111.01",
            yesterdayEarnings = if (address.isNullOrBlank()) "0" else "1.1",
            depositProducts = mutableListOf(
                DepositProductSummaryDTO(
                    productId = "1",
                    productName = "VLSUSD",
                    productDesc = "持币生息的VLSUSD",
                    productLogo = "",
                    depositYield = "3.7"
                ),
                DepositProductSummaryDTO(
                    productId = "2",
                    productName = "VLSEUR",
                    productDesc = "存生息，支持17个币种",
                    productLogo = "",
                    depositYield = "3.5"
                )
            ),
            borrowingProducts = mutableListOf(
                BorrowingProductSummaryDTO(
                    productId = "1",
                    productName = "VLSUSD",
                    productDesc = "质押挖矿",
                    productLogo = "",
                    borrowingRate = "3.7"
                ),
                BorrowingProductSummaryDTO(
                    productId = "2",
                    productName = "VLSEUR",
                    productDesc = "借生息，支持17个币种",
                    productLogo = "",
                    borrowingRate = "3.5"
                )
            )
        )
    }
}