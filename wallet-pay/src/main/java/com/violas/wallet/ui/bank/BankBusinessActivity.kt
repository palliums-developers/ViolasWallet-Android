package com.violas.wallet.ui.bank

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.palliums.extensions.expandTouchArea
import com.palliums.utils.getResourceId
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.utils.loadRoundedImage
import kotlinx.android.synthetic.main.activity_bank_business.*
import kotlinx.android.synthetic.main.item_manager_assert.view.*
import kotlinx.android.synthetic.main.view_bank_business_parameter.view.*
import kotlinx.android.synthetic.main.view_bank_business_parameter.view.tvContent
import kotlinx.android.synthetic.main.view_bank_business_parameter.view.tvTitle

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

    private fun handleBusinessUserView(
        viewGroup: ViewGroup,
        iconView: View,
        titleView: TextView,
        amountView: TextView,
        userAmountInfo: BusinessUserAmountInfo?
    ) {
        if (userAmountInfo == null) return
        viewGroup.visibility = View.VISIBLE
        iconView.setBackgroundResource(userAmountInfo.icon)
        titleView.text = userAmountInfo.title
        val content = if (userAmountInfo.value2 == null) {
            userAmountInfo.value1 + userAmountInfo.unit
        } else {
            userAmountInfo.value1 + "/" + userAmountInfo.value2 + userAmountInfo.unit
        }
        amountView.text = content
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBankBusinessViewModel.mPageTitleLiveData.observe(this, Observer {
            title = it
        })
        mBankBusinessViewModel.mBusinessUserInfoLiveData.observe(this, Observer { businessInfo ->
            tvBusinessName.text = businessInfo.businessName
            editBusinessValue.hint = businessInfo.businessInputHint

            handleBusinessUserView(
                viewGroupBusinessUsableAmount,
                ivBusinessUsableAmount,
                tvBusinessUsableAmountTitle,
                tvBusinessUsableAmount,
                businessInfo.businessUsableAmount
            )
            handleBusinessUserView(
                viewGroupBusinessLimitAmount,
                ivBusinessLimitAmount,
                tvBusinessLimitAmountTitle,
                tvBusinessLimitAmount,
                businessInfo.businessLimitAmount
            )
        })
        mBankBusinessViewModel.mCurrentAssetsLiveData.observe(this, Observer {
            ivCurrentAssetsName.text = it.getAssetsName()
            ivCurrentAssetsIcon.ivCoinLogo.loadRoundedImage(
                it.getLogoUrl(),
                getResourceId(R.attr.iconCoinDefLogo, baseContext),
                14
            )
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

        ivProductInfo.expandTouchArea(20)
        ivProductInfo.setOnClickListener {
            expandLayoutProductInfo.toggleExpand()
            val resId = if (expandLayoutProductInfo.isExpand) {
                R.drawable.icon_bank_info_expand
            } else {
                R.drawable.icon_bank_info_fold
            }
            ivProductInfo.setBackgroundResource(resId)
        }
        mBankBusinessViewModel.mProductExplanationListLiveData.observe(this, Observer {
            expandLayoutProductInfo.removeAllViews()
            it.forEach { productInfo ->
                val inflate = layoutInflater.inflate(
                    R.layout.view_item_expand_product_info,
                    null
                )
                inflate.tvTitle.text = productInfo.title
                inflate.tvContent.text = productInfo.content
                expandLayoutProductInfo.addView(inflate)
            }
            expandLayoutProductInfo.initExpand(true)
            expandLayoutProductInfo.reSetViewDimensions()
        })

        ivProductIssue.expandTouchArea(20)
        ivProductIssue.setOnClickListener {
            expandLayoutProductIssue.toggleExpand()
            val resId = if (expandLayoutProductIssue.isExpand) {
                R.drawable.icon_bank_info_expand
            } else {
                R.drawable.icon_bank_info_fold
            }
            ivProductIssue.setBackgroundResource(resId)
        }
        mBankBusinessViewModel.mFAQListLiveData.observe(this, Observer {
            expandLayoutProductIssue.removeAllViews()
            it.forEach { productIssue ->
                val inflate = layoutInflater.inflate(
                    R.layout.view_item_expand_product_info,
                    null
                )
                inflate.tvTitle.text = productIssue.q
                inflate.tvContent.text = productIssue.a
                expandLayoutProductIssue.addView(inflate)
            }
            expandLayoutProductIssue.reSetViewDimensions()
            expandLayoutProductIssue.collapse()
        })

        mBankBusinessViewModel.mBusinessPolicyLiveData.observe(this, Observer {
            if (it == null) {
                viewGroupAgreePolicy.visibility = View.GONE
                return@Observer
            }
            btnHasAgreePolicy.expandTouchArea(20)
            viewGroupAgreePolicy.visibility = View.VISIBLE
            tvPolicy.setMovementMethod(LinkMovementMethod.getInstance())
            tvPolicy.text = it
        })
        mBankBusinessViewModel.mBusinessActionHintLiveData.observe(this, Observer {
            if (it == null) {
                tvError.visibility = View.GONE
                return@Observer
            }
            tvError.visibility = View.VISIBLE
            tvError.text = it
        })
        mBankBusinessViewModel.mBusinessActionLiveData.observe(this, Observer {
            btnOperationAction.text = it
        })

        tvAllValue.setOnClickListener { clickSendAll() }
    }

    abstract fun clickSendAll()
}