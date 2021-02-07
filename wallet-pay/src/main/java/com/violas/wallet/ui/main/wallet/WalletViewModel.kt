package com.violas.wallet.ui.main.wallet

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.event.ReceiveIncentiveRewardsEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.math.BigDecimal
import java.math.RoundingMode

class WalletViewModel : ViewModel() {
    val mTotalFiatBalanceStrLiveData = MediatorLiveData<String>()

    private val mTotalFiatBalanceLiveData = MutableLiveData(BigDecimal("0"))
    val mHiddenTotalFiatBalanceLiveData = MutableLiveData(false)
    val receiveIncentiveRewardsStateLiveData = MutableLiveData(-1)

    private val incentiveService by lazy {
        DataRepository.getIncentiveService()
    }

    init {
        EventBus.getDefault().register(this)

        val calculate = {
            when {
                mHiddenTotalFiatBalanceLiveData.value == true -> {
                    mTotalFiatBalanceStrLiveData.value = "****"
                }
                mTotalFiatBalanceLiveData.value == BigDecimal("0") -> {
                    mTotalFiatBalanceStrLiveData.value = "$ 0.00"
                }
                else -> {
                    mTotalFiatBalanceStrLiveData.value =
                        "$ ${
                            mTotalFiatBalanceLiveData.value?.setScale(2, RoundingMode.DOWN)
                                ?.toPlainString() ?: "0.00"
                        }"
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

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
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

    fun resetReceiveIncentiveRewardsState() {
        val lastState = receiveIncentiveRewardsStateLiveData.value
        if (lastState == -1) return

        receiveIncentiveRewardsStateLiveData.value = -1
    }

    fun loadReceiveIncentiveRewardsState() {
        val lastState = receiveIncentiveRewardsStateLiveData.value
        if (lastState == 1 || lastState == Int.MIN_VALUE) return

        viewModelScope.launch {
            // 标记在加载中
            receiveIncentiveRewardsStateLiveData.value = Int.MIN_VALUE

            val newState = withContext(Dispatchers.IO) {
                try {
                    val accountManager =
                        WalletAppViewModel.getViewModelInstance().mAccountManager
                    val violasAccount =
                        accountManager.getIdentityByCoinType(getViolasCoinType().coinNumber())

                    incentiveService.getReceiveIncentiveRewardsState(violasAccount!!.address)
                } catch (e: Exception) {
                    lastState
                }
            }

            if (receiveIncentiveRewardsStateLiveData.value == Int.MIN_VALUE) {
                receiveIncentiveRewardsStateLiveData.value = newState
            } else {
                receiveIncentiveRewardsStateLiveData.value = -1
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceiveIncentiveRewardsEvent(event: ReceiveIncentiveRewardsEvent) {
        receiveIncentiveRewardsStateLiveData.value = 1
    }
}