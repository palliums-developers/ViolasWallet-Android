package com.violas.wallet.ui.main.wallet

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.violas.wallet.viewModel.bean.AssetsVo
import java.math.BigDecimal
import java.math.RoundingMode

class WalletViewModel : ViewModel() {
    val mTotalFiatBalanceStrLiveData = MediatorLiveData<String>()

    private val mTotalFiatBalanceLiveData = MutableLiveData<Long>(0)
    val mHiddenTotalFiatBalanceLiveData = MutableLiveData(false)

    init {
        val calculate = {
            when {
                mHiddenTotalFiatBalanceLiveData.value == true -> {
                    mTotalFiatBalanceStrLiveData.value = "$ ****"
                }
                mTotalFiatBalanceLiveData.value == 0L -> {
                    mTotalFiatBalanceStrLiveData.value = "$ 0.00"
                }
                else -> {
                    val balance = BigDecimal(mTotalFiatBalanceLiveData.value.toString()).divide(
                        BigDecimal("100"),
                        RoundingMode.DOWN
                    ).toPlainString()
                    mTotalFiatBalanceStrLiveData.value = "$ $balance"
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

    fun calculateFiat(it: List<AssetsVo>?) {
        var total = 0L
        it?.forEach {
            total += it.fiatAmountWithUnit.amount
        }
        mTotalFiatBalanceLiveData.value = total
    }
}