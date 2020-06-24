package com.violas.wallet.ui.main.wallet

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class WalletViewModel : ViewModel() {
    val mTotalFiatBalanceStrLiveData = MediatorLiveData<String>()

    private val mTotalFiatBalanceLiveData = MutableLiveData<Double>(0.0)
    val mHiddenTotalFiatBalanceLiveData = MutableLiveData(false)

    init {
        val calculate = {
            when {
                mHiddenTotalFiatBalanceLiveData.value == true -> {
                    mTotalFiatBalanceStrLiveData.value = "$ ****"
                }
                mTotalFiatBalanceLiveData.value == 0.0 -> {
                    mTotalFiatBalanceStrLiveData.value = "$ 0.00"
                }
                else -> {
                    mTotalFiatBalanceStrLiveData.value = "$ ${mTotalFiatBalanceLiveData.value}"
                }
            }
        }
        mTotalFiatBalanceStrLiveData.addSource(mTotalFiatBalanceLiveData) {
            calculate()
        }
        mTotalFiatBalanceStrLiveData.addSource(mHiddenTotalFiatBalanceLiveData) {
            calculate()
        }
    }

    fun taggerTotalDisplay() {
        mHiddenTotalFiatBalanceLiveData.value = !(mHiddenTotalFiatBalanceLiveData.value ?: false)
    }

    fun calculateFiat(it: List<AssetsVo>?) = viewModelScope.launch {
//        try {
            var total = 0.0
            it?.forEach {
                total += it.fiatAmountWithUnit.amount.toDouble()
            }
            mTotalFiatBalanceLiveData.value = total
//        } catch (e: Exception) {
//            mTotalFiatBalanceLiveData.value = 0.0
//        }

    }
}