package com.violas.wallet.ui.bank

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import kotlinx.android.synthetic.main.activity_bank_business.*
import kotlinx.android.synthetic.main.view_bank_business_parameter.view.*

/**
 * Created by QuincySx on 2020/8/21 15:28.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 数字银行-存款/借款业务
 */
abstract class BankBusinessActivity : BaseAppActivity() {
    protected val mBankBusinessViewModel by lazy {
        ViewModelProvider(this).get(BankBusinessViewModel::class.java)
    }

    override fun getLayoutResId() = R.layout.activity_bank_business

    override fun getTitleStyle() = PAGE_STYLE_SECONDARY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBankBusinessViewModel.mPageTitleLiveData.observe(this, Observer {
            title = it
        })
        mBankBusinessViewModel.mBusinessNameLiveData.observe(this, Observer {
            tvBusinessName.text = it
        })
        mBankBusinessViewModel.mBusinessHintLiveData.observe(this, Observer {
            editBusinessValue.hint = it
        })
        mBankBusinessViewModel.mBusinessUsableAmountLiveData.observe(this, Observer {
            viewGroupBusinessUsableAmount.visibility = View.VISIBLE
            ivBusinessUsableAmount.setBackgroundResource(it.icon)
            tvBusinessUsableAmountTitle.text = it.title
            val content = if (it.value2 == null) {
                it.value1 + it.unit
            } else {
                it.value1 + "/" + it.value2 + it.unit
            }
            tvBusinessUsableAmount.text = content
        })
        mBankBusinessViewModel.mBusinessLimitAmountLiveData.observe(this, Observer {
            it?.let {
                viewGroupBusinessLimitAmount.visibility = View.VISIBLE
                ivBusinessLimitAmount.setBackgroundResource(it.icon)
                tvBusinessLimitAmountTitle.text = it.title
                val content = if (it.value2 == null) {
                    it.value1 + it.unit
                } else {
                    it.value1 + "/" + it.value2 + it.unit
                }
                tvBusinessLimitAmount.text = content
            }
        })
        mBankBusinessViewModel.mBusinessParameterListLiveData.observe(this, Observer {
            viewGroupBusinessParameter.removeAllViews()
            it.forEach { businessParameter ->
                val inflate = layoutInflater.inflate(
                    R.layout.view_bank_business_parameter,
                    null
                )
                inflate.tvTitle.text = businessParameter.title
                inflate.tvContent.text = businessParameter.content
                businessParameter.declare?.let {
                    inflate.tvDeclare.visibility = View.VISIBLE
                    inflate.tvDeclare.text = businessParameter.declare
                }
                businessParameter.contentColor?.let {
                    inflate.tvContent.setTextColor(it)
                }
                viewGroupBusinessParameter.addView(inflate)
            }
        })
        mBankBusinessViewModel.mBusinessActionLiveData.observe(this, Observer {
            btnOperationAction.text = it
        })
    }
}