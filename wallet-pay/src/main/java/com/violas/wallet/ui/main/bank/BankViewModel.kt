package com.violas.wallet.ui.main.bank

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.palliums.base.BaseViewModel

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

    init {
        totalDepositLiveData.addSource(showAmountLiveData) {
            if (it) {
                totalDepositLiveData.value = "≈ 0.00"
                totalBorrowableLiveData.value = "≈ 0.00"
                totalEarningsLiveData.value = "≈ 0.00"
                yesterdayEarningsLiveData.value = "0.00 $"
            } else {
                totalDepositLiveData.value = "≈ ******"
                totalBorrowableLiveData.value = "≈ ******"
                totalEarningsLiveData.value = "≈ ******"
                yesterdayEarningsLiveData.value = "***"
            }
        }
    }

    fun toggleAmountShowHide() {
        showAmountLiveData.value = !(showAmountLiveData.value ?: true)
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        TODO("Not yet implemented")
    }
}