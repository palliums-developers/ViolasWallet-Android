package com.violas.wallet.ui.bank.deposit

import android.graphics.Color
import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.ui.bank.BankBusinessActivity
import com.violas.wallet.ui.bank.BusinessParameter
import com.violas.wallet.ui.bank.BusinessUserAmountInfo

/**
 * Created by elephant on 2020/8/19 15:38.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 数字银行-存款页面
 */
class DepositActivity : BankBusinessActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBankBusinessViewModel.mPageTitleLiveData.value = "存款"
        mBankBusinessViewModel.mBusinessNameLiveData.value = "我要存"
        mBankBusinessViewModel.mBusinessHintLiveData.value = "500 V-AAA起，每1V-AAA递增"
        mBankBusinessViewModel.mBusinessActionLiveData.value = "立即存款"
        mBankBusinessViewModel.mBusinessUsableAmountLiveData.value = BusinessUserAmountInfo(
            R.drawable.icon_bank_user_amount_info, "可用余额 :", "0", "VLSUSD"
        )
        mBankBusinessViewModel.mBusinessLimitAmountLiveData.value = BusinessUserAmountInfo(
            R.drawable.icon_bank_user_amount_limit, "每日限额 :", "1000", "VLSUSD", "1000"
        )
        mBankBusinessViewModel.mBusinessParameterListLiveData.value = arrayListOf(
            BusinessParameter("存款年利率", "0.50%", contentColor = Color.parseColor("#13B788")),
            BusinessParameter("质押率", "50%", "质押率=借贷数量/存款数量")
        )
    }
}