package com.violas.wallet.ui.main.wallet

import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class WalletViewModel : ViewModel() {
    val mTotalFiatBalanceStrLiveData = MediatorLiveData<String>()

    private val mTotalFiatBalanceLiveData = MutableLiveData(BigDecimal("0"))
    val mHiddenTotalFiatBalanceLiveData = MutableLiveData(false)

    init {
        val calculate = {
            when {
                mHiddenTotalFiatBalanceLiveData.value == true -> {
                    mTotalFiatBalanceStrLiveData.value = "$ ****"
                }
                mTotalFiatBalanceLiveData.value == BigDecimal("0") -> {
                    mTotalFiatBalanceStrLiveData.value = "$ 0.00"
                }
                else -> {
                    mTotalFiatBalanceStrLiveData.value =
                        "$ ${mTotalFiatBalanceLiveData.value?.setScale(2, RoundingMode.DOWN)
                            ?.toPlainString() ?: "0.00"}"
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
        var total = BigDecimal("0")
        it?.forEach {
            try {
                total = total.add(BigDecimal(it.fiatAmountWithUnit.amount))
            } catch (e: Exception) {
            }
        }
        mTotalFiatBalanceLiveData.value = total
    }
}