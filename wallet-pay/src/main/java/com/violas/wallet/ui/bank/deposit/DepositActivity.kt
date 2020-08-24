package com.violas.wallet.ui.bank.deposit

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import com.quincysx.crypto.bip44.CoinType
import com.violas.wallet.R
import com.violas.wallet.ui.bank.*

/**
 * Created by elephant on 2020/8/19 15:38.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 数字银行-存款页面
 */
class DepositActivity : BankBusinessActivity() {
    companion object {
        fun start(
            context: Context,
            coinType: CoinType,
            module: String? = null,
            address: String? = null,
            name: String? = null
        ) {

        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBankBusinessViewModel.mPageTitleLiveData.value = "存款"
        mBankBusinessViewModel.mBusinessUserInfoLiveData.value = BusinessUserInfo(
            "我要存", "500 V-AAA起，每1V-AAA递增",
            BusinessUserAmountInfo(
                R.drawable.icon_bank_user_amount_info, "可用余额 :", "0", "VLSUSD"
            ),
            BusinessUserAmountInfo(
                R.drawable.icon_bank_user_amount_limit, "每日限额 :", "1000", "VLSUSD", "1000"
            )
        )
        mBankBusinessViewModel.mBusinessActionLiveData.value = "立即存款"
        mBankBusinessViewModel.mBusinessParameterListLiveData.value = arrayListOf(
            BusinessParameter("存款年利率", "0.50%", contentColor = Color.parseColor("#13B788")),
            BusinessParameter("质押率", "50%", "质押率=借贷数量/存款数量")
        )
        mBankBusinessViewModel.mProductExplanationListLiveData.value = arrayListOf(
            ProductExplanation(
                "清算率",
                "清算率是指***************************************************************"
            ),
            ProductExplanation(
                "清算罚金",
                "清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金"
            )
        )
        mBankBusinessViewModel.mFAQListLiveData.value = arrayListOf(
            FAQ(
                "清算率",
                "清算率是指***************************************************************"
            ),
            FAQ(
                "清算罚金",
                "清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金清算发生时，对借贷债务额外收取的罚金"
            )
        )
    }

    override fun clickSendAll() {

    }
}